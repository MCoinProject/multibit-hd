/**
 * Copyright 2014 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.multibit.hd.core.managers;

import com.google.bitcoin.core.Wallet;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.dto.BackupSummary;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.dto.WalletIdTest;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.utils.Dates;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class BackupManagerTest {

  @Before
  public void setUp() throws Exception {

    Configurations.currentConfiguration = Configurations.newDefaultConfiguration();

  }

  @Test
  public void testBackupWallet() throws IOException {

    // Create a random temporary directory to act as the application directory containing wallets
    File temporaryApplicationDirectory = WalletManagerTest.makeRandomTemporaryApplicationDirectory();

    // Create a random temporary directory in which to story the cloudBackups
    File temporaryBackupDirectory = WalletManagerTest.makeRandomTemporaryApplicationDirectory();

    BackupManager backupManager = BackupManager.INSTANCE;

    // Initialise the backupManager to point at the temporaryBackupDirectory
    backupManager.initialise(temporaryApplicationDirectory, Optional.of(temporaryBackupDirectory));

    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(WalletIdTest.SEED_PHRASE_1));
    long nowInSeconds = Dates.nowInSeconds();
    String password = "credentials";
    WalletSummary walletSummary = WalletManager
      .INSTANCE
      .getOrCreateWalletSummary(
        temporaryApplicationDirectory,
        seed,
        nowInSeconds,
        password,
        "Example",
        "Example"
      );

    // Wallet manager does not initiate the backup
    BackupManager.INSTANCE.createRollingBackup(walletSummary, password);
    BackupManager.INSTANCE.createLocalBackup(walletSummary.getWalletId(), password);
    BackupManager.INSTANCE.createCloudBackup(walletSummary.getWalletId(), password);

    // Check there are initially a single wallet backup for the wallet id of the created wallet
    List<BackupSummary> localBackups = BackupManager.INSTANCE.getLocalZipBackups(walletSummary.getWalletId());

    assertThat(localBackups).isNotNull();
    assertThat(localBackups.size()).isEqualTo(1);

    List<BackupSummary> cloudBackups = BackupManager.INSTANCE.getCloudBackups(walletSummary.getWalletId(), temporaryBackupDirectory);
    assertThat(cloudBackups).isNotNull();
    assertThat(cloudBackups.size()).isEqualTo(1);

    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

    // Backup the wallet.
    // This zips the wallet root directory and adds a timestamp, then saves the file in both the local and cloud backup directories
    File localBackupFile = BackupManager.INSTANCE.createLocalBackup(walletSummary.getWalletId(), password);
    BackupManager.INSTANCE.createCloudBackup(walletSummary.getWalletId(), password);


    // Check that a backup copy has been saved in the local backup directory
    localBackups = BackupManager.INSTANCE.getLocalZipBackups(walletSummary.getWalletId());
    assertThat(localBackups).isNotNull();
    assertThat(localBackups.size()).isEqualTo(2);

    // Check that a backup copy has been saved in the cloud backup directory
    cloudBackups = BackupManager.INSTANCE.getCloudBackups(walletSummary.getWalletId(), temporaryBackupDirectory);
    assertThat(cloudBackups).isNotNull();
    assertThat(cloudBackups.size()).isEqualTo(2);

    // Load in the wallet backup and compare the wallets
    WalletId recreatedWalletId= BackupManager.INSTANCE.loadZipBackup(localBackupFile, Bip39SeedPhraseGenerator.split(WalletIdTest.SEED_PHRASE_1));
    assertThat(walletSummary.getWalletId()).isEqualTo(recreatedWalletId);

    // Open
    String walletRoot = WalletManager.createWalletRoot(recreatedWalletId);
    File walletDirectory = WalletManager.getOrCreateWalletDirectory(temporaryApplicationDirectory, walletRoot);
    WalletSummary recreatedWalletSummary = WalletManager.INSTANCE.loadFromWalletDirectory(walletDirectory, password);
    assertThat(recreatedWalletSummary).isNotNull();
    assertThat(recreatedWalletSummary.getWallet()).isNotNull();

    // Load one of the rolling backups
    Wallet wallet = BackupManager.INSTANCE.loadRollingBackup(walletSummary.getWalletId(), password);
    assertThat(wallet).isNotNull();
  }
}