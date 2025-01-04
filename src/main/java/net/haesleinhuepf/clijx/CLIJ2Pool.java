package net.haesleinhuepf.clijx;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij2.CLIJ2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a pool of CLIJ2 instances.
 * <p>
 * -----------------
 * Example usage:
 * CLIJ2Pool pool = new CLIJ2Pool("A500", "Intel");
 * Within a thread:
 * // Gets a available clij2 instance
 * CLIJ2 clij2 = pool.acquire();
 * clij2.push(
 * ...
 * // Returns the instance to the pool
 * pool.recycle(clij2);
 */
public class CLIJ2Pool extends ResourcePool<CLIJ2> {

    Logger logger = LoggerFactory.getLogger(CLIJ2Pool.class);

    AtomicInteger currentIndex = new AtomicInteger(0);
    final String[] gpuIds;

    /**
     * Creates a pool of CLIJ2 instances that can be used for parallel multi gpu processing
     * @param gpuIds gpu id array, can also be empty in which case a single GPU will be used
     */
    public CLIJ2Pool(String... gpuIds) {
        super(Math.max(1,gpuIds.length), false);
        if (gpuIds.length==0) {
            if (CLIJ.getAvailableDeviceNames().isEmpty()) {
                throw new RuntimeException("No CLIJ compatible device found");
            }
            logger.info("Empty pool, using device "+CLIJ.getAvailableDeviceNames().get(0));
            this.gpuIds = new String[]{CLIJ.getAvailableDeviceNames().get(0)};
        } else {
            this.gpuIds = gpuIds;
        }
        this.createPool();
    }

    @Override
    protected CLIJ2 createObject() {
        int index = currentIndex.addAndGet(1)-1;
        logger.info("CLIJ Pool; Initialization of device "+gpuIds[index]);
        // Note: we can't use CLIJ2.getInstance because it closes the context of the previous CLIJ instance created
        CLIJ2 clij2 = new CLIJ2(new CLIJ(gpuIds[index]));
        return clij2;
    }
}
