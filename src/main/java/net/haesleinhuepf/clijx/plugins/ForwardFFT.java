package net.haesleinhuepf.clijx.plugins;

import org.jocl.NativePointerObject;
import org.scijava.plugin.Plugin;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasAuthor;
import net.haesleinhuepf.clij2.utilities.HasClassifiedInputOutput;
import net.haesleinhuepf.clij2.utilities.IsCategorized;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_forwardFFT")
public class ForwardFFT  extends AbstractCLIJ2Plugin implements
CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor, HasClassifiedInputOutput, IsCategorized
{
	
	@Override
	public boolean executeCL() {
		boolean result = runFFT(getCLIJ2(), (ClearCLBuffer) (args[0]), (ClearCLBuffer) (args[1]));
		return result;
	}
	
	/**
	 * Run FFT on a CLBuffer
	 * 
	 * @param gpuImg input CLBuffer (needs to be pre-extended to an FFT friendly
	 *          size this can be done by using the padInputAndPush function)
	 * @return - output FFT as CLBuffer
	 */
	public static boolean runFFT(CLIJ2 clij2, ClearCLBuffer gpuImg, ClearCLBuffer gpuFFT) {
		
		ClearCLBuffer input_float = gpuImg;
		
		boolean input_converted=false;
		
		// currently we only support float type
		if (input_float.getNativeType() != NativeTypeEnum.Float) {
			input_float = clij2.create(gpuImg.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(gpuImg, input_float);
			input_converted=true;
		}

		// get the long pointers to in, out, context and queue.
		long l_in = ((NativePointerObject) (input_float.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_out = ((NativePointerObject) (gpuFFT.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();

		if (gpuImg.getDimensions().length==2) {
			// call the native code that runs the FFT
			clij2fftWrapper.fft2d_32f_lp((long) (gpuImg.getWidth()), gpuImg.getHeight(),
					l_in, l_out, l_context, l_queue);
		}
		
		if (gpuImg.getDimensions().length==3) {
			// call the native code that runs the FFT
			clij2fftWrapper.fft3d_32f_lp((long) (gpuImg.getWidth()), gpuImg.getHeight(), gpuImg.getDepth(),
					l_in, l_out, l_context, l_queue);
		}
		
		// if we had to convert the input deallocate the converted
		if (input_converted) {
			input_float.close();
		}
		
		return true;
	}
	
	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		
		long[] dimensions=new long[in.getDimensions().length];
		
		dimensions[0]=2*(in.getDimensions()[0]/2+1);
		
		for (int d=1;d<in.getDimensions().length;d++) {
			dimensions[d]=in.getDimensions()[d];
		}
		
		return getCLIJ2().create(dimensions, NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, ByRef Image destination";
	}

	@Override
	public String getDescription() {
		return "Performs forward FFT";
	}

	@Override
	public String getAvailableForDimensions() {
		return "2D, 3D";
	}

	@Override
	public String getAuthorName() {
		return "Brian Northan";
	}

	@Override
	public String getInputType() {
		return "Image";
	}

	@Override
	public String getOutputType() {
		return "Image";
	}

	@Override
	public String getCategories() {
		return "Filter";
	}

}
