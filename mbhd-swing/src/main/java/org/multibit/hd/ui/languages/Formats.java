package org.multibit.hd.ui.languages;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.config.LanguageConfiguration;
import org.multibit.hd.core.events.TransactionSeenEvent;
import org.multibit.hd.core.utils.BitcoinSymbol;
import org.multibit.hd.core.utils.Coins;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * <p>Utility to provide the following to controllers:</p>
 * <ul>
 * <li>Access to international formats for date/time and decimal data</li>
 * <li>Access to alert layouts in different languages</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class Formats {

  /**
   * The separator character to use between currencies in an exchange rate
   */
  public static String EXCHANGE_RATE_SEPARATOR = " / ";

  /**
   * The number of decimal places for showing the exchange rate depends on the bitcoin symbol used, with this offset
   */
  public static int EXCHANGE_RATE_DECIMAL_PLACES_OFFSET = 2;

  /**
   * <p>Provide a split representation for the Bitcoin balance display.</p>
   * <p>For example, 12345.6789 becomes "12,345.67", "89" </p>
   * <p>The amount will be adjusted by the symbolic multiplier from the current confiuration</p>
   *
   * @param coin                  The amount in coins
   * @param languageConfiguration The  language configuration to use as the basis for presentation
   * @param bitcoinConfiguration  The Bitcoin configuration to use as the basis for the symbol
   *
   * @return The left [0] and right [1] components suitable for presentation as a balance with no symbolic decoration
   */
  public static String[] formatCoinAsSymbolic(
    Coin coin,
    LanguageConfiguration languageConfiguration,
    BitcoinConfiguration bitcoinConfiguration
  ) {
    return formatCoinAsSymbolic(coin, languageConfiguration, bitcoinConfiguration, true);
  }

  /**
   * <p>Provide a split representation for the Bitcoin balance display.</p>
   * <p>For example, 123456789 in uBTC becomes "1,234,567.", "89" </p>
   * <p>The amount will be adjusted by the symbolic multiplier from the current configuration</p>
   *
   * @param coin              The amount in coins
   * @param languageConfiguration The  language configuration to use as the basis for presentation
   * @param bitcoinConfiguration  The Bitcoin configuration to use as the basis for the symbol
   * @param showNegative          If true, show '-' for negative numbers
   *
   * @return The left [0] and right [1] components suitable for presentation as a balance with no symbolic decoration
   */
  public static String[] formatCoinAsSymbolic(
    Coin coin,
    LanguageConfiguration languageConfiguration,
    BitcoinConfiguration bitcoinConfiguration,
    boolean showNegative
  ) {

    Preconditions.checkNotNull(coin, "'coin' must be present");
    Preconditions.checkNotNull(languageConfiguration, "'languageConfiguration' must be present");
    Preconditions.checkNotNull(bitcoinConfiguration, "'bitcoinConfiguration' must be present");

    Locale currentLocale = languageConfiguration.getLocale();
    BitcoinSymbol bitcoinSymbol = BitcoinSymbol.of(bitcoinConfiguration.getBitcoinSymbol());

    DecimalFormatSymbols dfs = configureDecimalFormatSymbols(bitcoinConfiguration, currentLocale);
    DecimalFormat localFormat = configureBitcoinDecimalFormat(dfs, bitcoinSymbol, showNegative);

    // Apply formatting to the symbolic amount
    String formattedAmount = localFormat.format(Coins.toSymbolicAmount(coin, bitcoinSymbol));

    // The Satoshi symbol does not have decimals
    if (BitcoinSymbol.SATOSHI.equals(bitcoinSymbol)) {

      return new String[]{
        formattedAmount,
        ""
      };

    }

    // All other representations require a decimal

    int decimalIndex = formattedAmount.lastIndexOf(dfs.getDecimalSeparator());

    if (decimalIndex == -1) {
      formattedAmount += dfs.getDecimalSeparator() + "00";
      decimalIndex = formattedAmount.lastIndexOf(dfs.getDecimalSeparator());
    }

    return new String[]{
      formattedAmount.substring(0, decimalIndex + 3), // 12,345.67 (significant figures)
      formattedAmount.substring(decimalIndex + 3) // 89 (lesser figures truncated )
    };

  }

  /**
   * <p>Provide a single text representation for the Bitcoin balance display.</p>
   * <p>For example, 123456789 becomes "1,234,567.89 uBTC" or "uXBT 12,345.6789" </p>
   * <p>The amount will be adjusted by the symbolic multiplier from the current configuration</p>
   *
   * @param coin                  The amount in coins
   * @param languageConfiguration The  language configuration to use as the basis for presentation
   * @param bitcoinConfiguration  The Bitcoin configuration to use as the basis for the symbol
   *
   * @return The string suitable for presentation as a balance with symbol in a UTF-8 string
   */
  public static String formatCoinAsSymbolicText(
    Coin coin,
    LanguageConfiguration languageConfiguration,
    BitcoinConfiguration bitcoinConfiguration
  ) {

    String[] formattedAmount = formatCoinAsSymbolic(coin, languageConfiguration, bitcoinConfiguration);

    String lineSymbol = BitcoinSymbol.of(bitcoinConfiguration.getBitcoinSymbol()).getTextSymbol();

    // Convert to single text line with leading or trailing symbol
    if (bitcoinConfiguration.isCurrencySymbolLeading()) {
      return lineSymbol + "\u00a0" + formattedAmount[0] + formattedAmount[1];
    } else {
      return formattedAmount[0] + formattedAmount[1] + "\u00a0" + lineSymbol;
    }

  }


  /**
   * <p>Provide a simple representation for a local currency amount.</p>
   *
   * @param amount               The amount as a plain number (no multipliers)
   * @param locale               The locale to use
   * @param bitcoinConfiguration The Bitcoin configuration to use as the basis for the symbol
   * @param showNegative         True if the negative prefix is allowed
   *
   * @return The local currency representation with no symbolic decoration
   */
  public static String formatLocalAmount(BigDecimal amount, Locale locale, BitcoinConfiguration bitcoinConfiguration, boolean showNegative) {

    if (amount == null) {
      return "";
    }

    DecimalFormatSymbols dfs = configureDecimalFormatSymbols(bitcoinConfiguration, locale);
    DecimalFormat localFormat = configureLocalDecimalFormat(dfs, bitcoinConfiguration, showNegative);

    return localFormat.format(amount);

  }

  /**
   * <p>Convert the bitcoin exchange rate to use the unit of bitcoin being displayed</p>
   * <p>For example, 589.00 will become "0,589" if the unit of bitcoin is mB and the decimal separator is ","</p>
   * <p>The value passed into formatExchangeRate must be in "fiat currency per bitcoin" and NOT localised</p>
   *
   * @param exchangeRate          The exchange rate in fiat per bitcoin
   * @param languageConfiguration The  language configuration to use as the basis for presentation
   * @param bitcoinConfiguration  The Bitcoin configuration to use as the basis for the symbol
   *
   * @return The localised string representing the bitcoin exchange rate in the display bitcoin unit
   */
  public static String formatExchangeRate(
    Optional<String> exchangeRate,
    LanguageConfiguration languageConfiguration,
    BitcoinConfiguration bitcoinConfiguration
  ) {

    Preconditions.checkNotNull(exchangeRate, "'exchangeRate' must be non null");
    Preconditions.checkState(exchangeRate.isPresent(), "'exchangeRate' must be present");
    Preconditions.checkNotNull(languageConfiguration, "'languageConfiguration' must be present");
    Preconditions.checkNotNull(bitcoinConfiguration, "'bitcoinConfiguration' must be present");

    BigDecimal exchangeRateBigDecimal = new BigDecimal(exchangeRate.get());

    // Correct for non unitary bitcoin display units e.g 567 USD per BTCis identical to 0.567 USD per mBTC
    BigDecimal correctedExchangeRateBigDecimal = exchangeRateBigDecimal.divide(BitcoinSymbol.current().multiplier());

    Locale currentLocale = languageConfiguration.getLocale();

    DecimalFormatSymbols dfs = configureDecimalFormatSymbols(bitcoinConfiguration, currentLocale);
    DecimalFormat localFormat = configureLocalDecimalFormat(dfs, bitcoinConfiguration, false);

    localFormat.setMinimumFractionDigits(Formats.EXCHANGE_RATE_DECIMAL_PLACES_OFFSET + (int)Math.log10(BitcoinSymbol.current().multiplier().doubleValue()));
    return localFormat.format(correctedExchangeRateBigDecimal);
  }

  /**
   * @param dfs The decimal format symbols
   *
   * @return A decimal format suitable for Bitcoin balance representation
   */
  private static DecimalFormat configureBitcoinDecimalFormat(DecimalFormatSymbols dfs, BitcoinSymbol bitcoinSymbol, boolean showNegative) {

    DecimalFormat format = new DecimalFormat();

    format.setDecimalFormatSymbols(dfs);

    format.setMaximumIntegerDigits(16);
    format.setMinimumIntegerDigits(1);

    format.setMaximumFractionDigits(bitcoinSymbol.decimalPlaces());
    format.setMinimumFractionDigits(bitcoinSymbol.decimalPlaces());

    format.setDecimalSeparatorAlwaysShown(false);

    if (showNegative) {
      format.setNegativePrefix("-");
    } else {
      format.setNegativePrefix("");
    }

    return format;
  }

  /**
   * @param dfs                  The decimal format symbols
   * @param bitcoinConfiguration The Bitcoin configuration to use
   * @param showNegative         True if the negative prefix is allowed
   *
   * @return A decimal format suitable for local currency balance representation
   */
  private static DecimalFormat configureLocalDecimalFormat(DecimalFormatSymbols dfs, BitcoinConfiguration bitcoinConfiguration, boolean showNegative) {

    DecimalFormat format = new DecimalFormat();

    format.setDecimalFormatSymbols(dfs);

    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(bitcoinConfiguration.getLocalDecimalPlaces());
    format.setMinimumFractionDigits(bitcoinConfiguration.getLocalDecimalPlaces());

    format.setDecimalSeparatorAlwaysShown(true);

    if (showNegative) {
      format.setNegativePrefix("-");
    } else {
      format.setNegativePrefix("");
    }

    return format;
  }

  /**
   * @param bitcoinConfiguration The Bitcoin configuration
   * @param currentLocale        The current locale
   *
   * @return The decimal format symbols to use based on the configuration and locale
   */
  private static DecimalFormatSymbols configureDecimalFormatSymbols(BitcoinConfiguration bitcoinConfiguration, Locale currentLocale) {

    DecimalFormatSymbols dfs = new DecimalFormatSymbols(currentLocale);

    dfs.setDecimalSeparator(bitcoinConfiguration.getDecimalSeparator().charAt(0));
    dfs.setGroupingSeparator(bitcoinConfiguration.getGroupingSeparator().charAt(0));

    return dfs;

  }

  /**
   * @param event The "transaction seen" event
   *
   * @return A String suitably formatted for presentation as an alert message
   */
  public static String formatAlertMessage(TransactionSeenEvent event) {

    // Decode the "transaction seen" event
    final Coin amount = event.getAmount();

    final Coin modulusAmount;
    if (amount.compareTo(Coin.ZERO) >= 0) {
      modulusAmount = amount;
    } else {
      modulusAmount = amount.negate();
    }
    // Create a suitable representation for inline text (no icon)
    final String messageAmount = Formats.formatCoinAsSymbolicText(
            modulusAmount,
      Configurations.currentConfiguration.getLanguage(),
      Configurations.currentConfiguration.getBitcoin()
    );

    // Construct a suitable alert message
    if (amount.compareTo(Coin.ZERO) >= 0) {
      // Positive or zero amount, this is a receive
      return Languages.safeText(MessageKey.PAYMENT_RECEIVED_ALERT, messageAmount);
    } else {
      // Negative amount, this is a send (probably from a wallet clone elsewhere)
      return Languages.safeText(MessageKey.PAYMENT_SENT_ALERT, messageAmount);
    }
  }

  /**
   * @param bitcoinURI The Bitcoin URI
   *
   * @return A String suitably formatted for presentation as an alert message
   */
  public static Optional<String> formatAlertMessage(BitcoinURI bitcoinURI) {

    Optional<String> alertMessage = Optional.absent();

    // Decode the Bitcoin URI
    Optional<Address> address = Optional.fromNullable(bitcoinURI.getAddress());
    Optional<Coin> amount = Optional.fromNullable(bitcoinURI.getAmount());
    Optional<String> label = Optional.fromNullable(bitcoinURI.getLabel());

    // Only proceed if we have an address
    if (address.isPresent()) {

      final String messageAmount;
      if (amount.isPresent()) {
        // Create a suitable representation for inline text (no icon)
        messageAmount = Formats.formatCoinAsSymbolicText(
          amount.get(),
          Configurations.currentConfiguration.getLanguage(),
          Configurations.currentConfiguration.getBitcoin()
        );
      } else {
        messageAmount = Languages.safeText(MessageKey.NOT_AVAILABLE);
      }

      // Ensure we truncate the label if present
      String truncatedLabel = Languages.truncatedList(Lists.newArrayList(label.or(Languages.safeText(MessageKey.NOT_AVAILABLE))), 35);

      // Construct a suitable alert message
      alertMessage = Optional.of(Languages.safeText(MessageKey.BITCOIN_URI_ALERT, truncatedLabel, address.get().toString(), messageAmount));
    }

    return alertMessage;

  }

}
