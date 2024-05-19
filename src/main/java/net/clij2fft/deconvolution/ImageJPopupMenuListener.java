package net.clij2fft.deconvolution;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class ImageJPopupMenuListener implements PopupMenuListener {

    private final ImagePlusComboBox imagePlusCombo;

    public ImageJPopupMenuListener(ImagePlusComboBox imagePlusCombo) {
        this.imagePlusCombo = imagePlusCombo;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        // Update the list before the dropdown is shown
        imagePlusCombo.refreshList();
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // No action needed when the popup is canceled
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // No action needed when the popup is closed
    }
}