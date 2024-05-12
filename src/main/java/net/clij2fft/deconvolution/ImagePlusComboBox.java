package net.clij2fft.deconvolution;

import javax.swing.DefaultComboBoxModel;

import ij.ImagePlus;
import ij.WindowManager;

public class ImagePlusComboBox extends DefaultComboBoxModel<ImagePlus> {
	
	public ImagePlusComboBox() {
		super();
		refreshList();
	}
	
	public void refreshList() {
		removeAllElements();
		
		final int[] ids = WindowManager.getIDList();
		
		if (ids!=null) {
			for (int id: ids) {
				addElement(WindowManager.getImage(id));
			}
		}
	}
}
