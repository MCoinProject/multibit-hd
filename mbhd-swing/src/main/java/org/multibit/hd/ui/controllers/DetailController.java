package org.multibit.hd.ui.controllers;

import com.google.common.eventbus.Subscribe;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.ui.events.controller.ShowDetailScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Controller for the detail view that manages various screens</p>
 * <ul>
 * <li>Handles interaction between the model and the view</li>
 * </ul>
 */
public class DetailController {

  private static final Logger log = LoggerFactory.getLogger(DetailController.class);

  public DetailController() {

    CoreServices.uiEventBus.register(this);

  }

  /**
   * <p>Called when the detail screen should be should be changed</p>
   *
   * @param event The exchange rate change event
   */
  @Subscribe
  public void onDetailScreenChanged(ShowDetailScreenEvent event) {

    switch (event.getScreen()) {
      case MAIN_SETTINGS:

        break;
    }



    // Post the event

  }

}