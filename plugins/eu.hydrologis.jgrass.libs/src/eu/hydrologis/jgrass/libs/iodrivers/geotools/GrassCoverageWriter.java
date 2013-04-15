package eu.hydrologis.jgrass.libs.iodrivers.geotools;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.ViewType;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.operation.TransformException;

import eu.hydrologis.jgrass.libs.iodrivers.imageio.GrassBinaryImageWriter;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.io.core.GrassBinaryRasterWriteHandler;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.spi.GrassBinaryImageWriterSpi;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;

/**
 * Coverage Writer class for writing GRASS raster maps.
 * <p>
 * The class writes a GRASS raster map to a GRASS workspace (see package documentation for further
 * info). The writing is really done via Imageio extended classes.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see GrassBinaryImageWriter
 * @see GrassBinaryRasterWriteHandler
 */
public class GrassCoverageWriter {
    private File output;
    private final IProgressMonitorJGrass monitor;

    /**
     * Constructor for the {@link GrassCoverageWriter}.
     */
    public GrassCoverageWriter( File output, IProgressMonitorJGrass monitor ) {
        this.output = output;
        this.monitor = monitor;
    }

    /**
     * Writes the {@link GridCoverage2D supplied coverage} to disk.
     * <p>
     * Note that this also takes care to cloes the file handle after writing to disk.
     * </p>
     * 
     * @param gridCoverage2D the coverage to write.
     * @throws IOException
     */
    public void write( GridCoverage2D gridCoverage2D ) throws IOException {
        try {
            Envelope2D env = gridCoverage2D.getEnvelope2D();
            GridEnvelope2D worldToGrid = gridCoverage2D.getGridGeometry().worldToGrid(env);

            double xRes = env.getWidth() / worldToGrid.getWidth();
            double yRes = env.getHeight() / worldToGrid.getHeight();

            JGrassRegion region = new JGrassRegion(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), xRes, yRes);

            GrassBinaryImageWriterSpi writerSpi = new GrassBinaryImageWriterSpi();
            GrassBinaryImageWriter writer = new GrassBinaryImageWriter(writerSpi, monitor);
            monitor.beginTask("Retrieving image from coverage.", IProgressMonitorJGrass.UNKNOWN);
            RenderedImage renderedImage = gridCoverage2D.view(ViewType.GEOPHYSICS).getRenderedImage();
            monitor.done();
            writer.setOutput(output, region);
            writer.write(renderedImage);
            writer.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void write( GridCoverage2D gridCoverage2D, JGrassRegion writeRegion ) throws IOException {
        GrassBinaryImageWriterSpi writerSpi = new GrassBinaryImageWriterSpi();
        GrassBinaryImageWriter writer = new GrassBinaryImageWriter(writerSpi, monitor);
        monitor.beginTask("Retrieving image from coverage.", IProgressMonitorJGrass.UNKNOWN);
        RenderedImage renderedImage = gridCoverage2D.view(ViewType.GEOPHYSICS).getRenderedImage();
        monitor.done();
        writer.setOutput(output, writeRegion);
        writer.write(renderedImage);
        writer.dispose();
    }

}
