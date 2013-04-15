/*
 * $RCSfile: ImageReadCRIF.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/01 00:39:04 $
 * $State: Exp $
 */

package eu.hydrologis.jgrass.libs.jai.imageioimpl;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;

import com.sun.media.jai.operator.ImageReadDescriptor;

import eu.hydrologis.jgrass.libs.messages.Messages;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;
/**
 * This is a descriptor class of the operation which read the Grass ascii file.
 * <p>
 * 
 * @see {@link ImageReadDescriptor}
 *      </p>
 * @author daniele andreis
 * @since 1.1.0
 */
public class GrassFileReadOpImage extends OpImage {
    /**
     * The <code>ImageReadParam</code> used in reading the image.
     */
    private ImageReadParam param;

    /**
     * The <code>ImageReader</code> used to read the image.
     */
    private ImageReader reader;

    /**
     * The index of the image to be read.
     */
    private int imageIndex;

    /**
     * Whether stream metadata have been be read.
     */
    private boolean streamMetadataRead = false;

    /**
     * Whether image metadata have been be read.
     */
    private boolean imageMetadataRead = false;

    /**
     * A stream to be closed when the instance is disposed; may be null.
     */
    private ImageInputStream streamToClose;

    /**
     * Destination to source X scale factor.
     */
    private int scaleX;

    /**
     * Destination to source Y scale factor.
     */
    private int scaleY;

    /**
     * Destination to source X translation factor.
     */
    private int transX;

    /**
     * Destination to source Y translation factor.
     */
    private int transY;

    /**
     * Derive the image layout based on the user-supplied layout, reading parameters, and image
     * index.
     */
    private static ImageLayout layoutHelper( ImageLayout il, ImageReadParam param,
            ImageReader reader, int imageIndex ) throws IOException {
        ImageLayout layout = (il == null) ? new ImageLayout() : (ImageLayout) il.clone();

        // --- Determine the image type. ---

        // If not provided in the original layout, set the SampleModel
        // and ColorModel from the ImageReadParam, if supplied.
        if (!layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)
                && !layout.isValid(ImageLayout.COLOR_MODEL_MASK)) {
            // If an ImageReadParam has been supplied and has its
            // destinationType set then use it. Otherwise default to
            // the raw image type.
            ImageTypeSpecifier imageType = (param != null && param.getDestinationType() != null)
                    ? param.getDestinationType()
                    : reader.getRawImageType(imageIndex);

            // XXX The following block of code should not be necessary
            // but for J2SE 1.4.0 FCS ImageReader.getRawImageType(0)
            // returns null for earth.jpg, Bas-noir.jpg, etc.
            if (imageType == null) {
                Iterator imageTypes = reader.getImageTypes(imageIndex);
                while( imageType == null && imageTypes.hasNext() ) {
                    imageType = (ImageTypeSpecifier) imageTypes.next();
                }
            }

            // XXX Should an exception be thrown if imageType is null?
            if (imageType != null) {
                // Set the SampleModel and ColorModel.
                layout.setSampleModel(imageType.getSampleModel());
                layout.setColorModel(imageType.getColorModel());
            }
        }

        // --- Set up the destination bounds. ---

        // Calculate the computable destination bounds.
        Dimension sourceSize = getSourceSize(param, reader, imageIndex);
        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, sourceSize.width, sourceSize.height, layout.getMinX(null), // valid
                // value or
                // 0layoutH
                layout.getMinY(null), // valid value or 0
                false, srcRegion, destRegion);

        if (!destRegion.isEmpty()) {
            // Backup layout image bounds with computable bounds.
            if (!layout.isValid(ImageLayout.WIDTH_MASK)) {
                layout.setWidth(destRegion.width);
            }
            if (!layout.isValid(ImageLayout.HEIGHT_MASK)) {
                layout.setHeight(destRegion.height);
            }
            if (!layout.isValid(ImageLayout.MIN_X_MASK)) {
                layout.setMinX(destRegion.x);
            }
            if (!layout.isValid(ImageLayout.MIN_Y_MASK)) {
                layout.setMinY(destRegion.y);
            }

            // Ensure the layout bounds intersect computable bounds.
            Rectangle destBounds = new Rectangle(layout.getMinX(null), layout.getMinY(null), layout
                    .getWidth(null), layout.getHeight(null));
            if (destRegion.intersection(destBounds).isEmpty()) {
                throw new IllegalArgumentException(Messages
                        .getString("FileImageReadOpImage.intersectRegion"));
            }
        }

        // --- Set up the tile grid. ---

        if (!layout.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            layout.setTileGridXOffset(reader.getTileGridXOffset(imageIndex));
        }
        if (!layout.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            layout.setTileGridYOffset(reader.getTileGridYOffset(imageIndex));
        }
        if (!layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
            layout.setTileWidth(reader.getTileWidth(imageIndex));
        }
        if (!layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
            layout.setTileHeight(reader.getTileHeight(imageIndex));
        }

        return layout;
    }

    /**
     * Returns whether an <code>ImageTypeSpecifier</code> may be used to read in the image at a
     * specified index. XXX
     */
    private static boolean isCompatibleType( ImageTypeSpecifier imageType, ImageReader reader,
            int imageIndex ) throws IOException {
        Iterator imageTypes = reader.getImageTypes(imageIndex);

        boolean foundIt = false;
        while( imageTypes.hasNext() ) {
            ImageTypeSpecifier type = (ImageTypeSpecifier) imageTypes.next();
            if (type.equals(imageType)) {
                foundIt = true;
                break;
            }
        }

        return foundIt;
    }

    /**
     * Returns the source region to be read. If the sourceRenderSize is being used it is returned;
     * otherwise the raw source dimensions are returned. XXX
     */
    private static Dimension getSourceSize( ImageReadParam param, ImageReader reader, int imageIndex )
            throws IOException {
        Dimension sourceSize = null;
        if (param != null && param.canSetSourceRenderSize()) {
            sourceSize = param.getSourceRenderSize();
        }
        if (sourceSize == null) {
            sourceSize = new Dimension(reader.getWidth(imageIndex), reader.getHeight(imageIndex));
        }
        return sourceSize;
    }

    /**
     * XXX
     */
    // Code copied from ImageReader.java
    private static Rectangle getSourceRegion( ImageReadParam param, int srcWidth, int srcHeight ) {
        Rectangle sourceRegion = new Rectangle(0, 0, srcWidth, srcHeight);
        if (param != null) {
            Rectangle region = param.getSourceRegion();
            if (region != null) {
                sourceRegion = sourceRegion.intersection(region);
            }

            int subsampleXOffset = param.getSubsamplingXOffset();
            int subsampleYOffset = param.getSubsamplingYOffset();
            sourceRegion.x += subsampleXOffset;
            sourceRegion.y += subsampleYOffset;
            sourceRegion.width -= subsampleXOffset;
            sourceRegion.height -= subsampleYOffset;
        }

        return sourceRegion;
    }

    /**
     * XXX
     */
    // clipDestRegion: whether to clip destRegion to positive coordinates.
    // Code based on method of same name in ImageReader.java
    private static void computeRegions( ImageReadParam param, int srcWidth, int srcHeight,
            int destMinX, int destMinY, boolean clipDestRegion, Rectangle srcRegion,
            Rectangle destRegion ) {
        if (srcRegion == null) {
            throw new IllegalArgumentException("srcRegion == null");
        }
        if (destRegion == null) {
            throw new IllegalArgumentException("destRegion == null");
        }

        // Start with the entire source image
        srcRegion.setBounds(0, 0, srcWidth, srcHeight);

        // Destination also starts with source image, as that is the
        // maximum extent if there is no subsampling
        destRegion.setBounds(destMinX, destMinY, srcWidth, srcHeight);

        // Clip that to the param region, if there is one
        int periodX = 1;
        int periodY = 1;
        int gridX = 0;
        int gridY = 0;
        if (param != null) {
            Rectangle paramSrcRegion = param.getSourceRegion();
            if (paramSrcRegion != null) {
                srcRegion.setBounds(srcRegion.intersection(paramSrcRegion));
            }
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();
            gridX = param.getSubsamplingXOffset();
            gridY = param.getSubsamplingYOffset();
            srcRegion.translate(gridX, gridY);
            srcRegion.width -= gridX;
            srcRegion.height -= gridY;
            Point destinationOffset = param.getDestinationOffset();
            destRegion.translate(destinationOffset.x, destinationOffset.y);
        }

        if (clipDestRegion) {
            // Now clip any negative destination offsets, i.e. clip
            // to the top and left of the destination image
            if (destRegion.x < 0) {
                int delta = -destRegion.x * periodX;
                srcRegion.x += delta;
                srcRegion.width -= delta;
                destRegion.x = 0;
            }
            if (destRegion.y < 0) {
                int delta = -destRegion.y * periodY;
                srcRegion.y += delta;
                srcRegion.height -= delta;
                destRegion.y = 0;
            }
        }

        // Now clip the destination Region to the subsampled width and height
        int subsampledWidth = (srcRegion.width + periodX - 1) / periodX;
        int subsampledHeight = (srcRegion.height + periodY - 1) / periodY;
        destRegion.width = subsampledWidth;
        destRegion.height = subsampledHeight;

        if (srcRegion.isEmpty() || destRegion.isEmpty()) {
            throw new IllegalArgumentException(Messages
                    .getString("FileImageReadOpImage.voidRegion"));
        }
    }

    /**
     * XXX NB: This class may reset the following fields of the ImageReadParam destinationOffset
     * destinationType sourceRegion
     */
    GrassFileReadOpImage( ImageLayout layout, Map configuration, ImageReadParam param,
            ImageReader reader, int imageIndex, IProgressMonitorJGrass monitor,
            Boolean setSampling, Boolean castDtoF ) throws IOException {
        super(null, layoutHelper(layout, param, reader, imageIndex), configuration, false);

        // Revise parameter 'param' as needed.
        if (param == null) {
            // Get the ImageReadParam from the ImageReader.
            param = reader.getDefaultReadParam();
        } else if (param instanceof Cloneable) {
            this.param = param;
        } else if (param.getClass().getName().equals("javax.imageio.ImageReadParam")) {
            // The ImageReadParam passed in is non-null. As the
            // ImageReadParam class is not Cloneable, if the param
            // class is simply ImageReadParam, then create a new
            // ImageReadParam instance and set all its fields
            // which were set in param. This will eliminate problems
            // with concurrent modification of param for the cases
            // in which there is not a special ImageReadparam used.

            // Create a new ImageReadParam instance.
            ImageReadParam newParam = new ImageReadParam();

            // Set all fields which need to be set.

            // IIOParamController field.
            if (param.hasController()) {
                newParam.setController(param.getController());
            }

            // Destination fields.
            newParam.setDestination(param.getDestination());
            if (param.getDestinationType() != null) {
                // Set the destination type only if non-null as the
                // setDestinationType() clears the destination field.
                newParam.setDestinationType(param.getDestinationType());
            }
            newParam.setDestinationBands(param.getDestinationBands());
            newParam.setDestinationOffset(param.getDestinationOffset());

            // Source fields.
            newParam.setSourceBands(param.getSourceBands());
            newParam.setSourceRegion(param.getSourceRegion());
            if (param.getSourceMaxProgressivePass() != Integer.MAX_VALUE) {
                newParam.setSourceProgressivePasses(param.getSourceMinProgressivePass(), param
                        .getSourceNumProgressivePasses());
            }
            if (param.canSetSourceRenderSize()) {
                newParam.setSourceRenderSize(param.getSourceRenderSize());
            }
            newParam.setSourceSubsampling(param.getSourceXSubsampling(), param
                    .getSourceYSubsampling(), param.getSubsamplingXOffset(), param
                    .getSubsamplingYOffset());

            // Replace the local variable with the new ImageReadParam.
            param = newParam;
        }

        // Set instance variables from (possibly revised) parameters.
        this.param = param;
        this.reader = reader;
        this.imageIndex = imageIndex;

        // If an ImageTypeSpecifier is specified in the ImageReadParam
        // but it is incompatible with the ImageReader, then attempt to
        // replace it with a compatible one derived from this image.
        if (param.getDestinationType() != null
                && !isCompatibleType(param.getDestinationType(), reader, imageIndex)
                && sampleModel != null && colorModel != null) {
            ImageTypeSpecifier newImageType = new ImageTypeSpecifier(colorModel, sampleModel);
            if (isCompatibleType(newImageType, reader, imageIndex)) {
                param.setDestinationType(newImageType);
            }
        }

        // --- Compute the destination to source mapping coefficients. ---

        Dimension sourceSize = getSourceSize(param, reader, imageIndex);

        Rectangle srcRegion = getSourceRegion(param, sourceSize.width, sourceSize.height);

        Point destinationOffset = this.param.getDestinationOffset();

        this.scaleX = this.param.getSourceXSubsampling();
        this.scaleY = this.param.getSourceYSubsampling();
        this.transX = srcRegion.x + this.param.getSubsamplingXOffset()
                - this.param.getSourceXSubsampling() * (minX + destinationOffset.x);
        this.transY = srcRegion.y + this.param.getSubsamplingYOffset()
                - this.param.getSourceYSubsampling() * (minY + destinationOffset.y);

        // Replace the original destination offset with (0,0) as the
        // destination-to-source mapping assimilates this value.
        this.param.setDestinationOffset(new Point());
        // XXX Need to unset other ImageReadParam settings either here
        // or in computeTile(). Examine this issue taking into account
        // synchronization.

        // Set the ImageReadParam property.
        setProperty(ImageReadDescriptor.PROPERTY_NAME_IMAGE_READ_PARAM, param);

        // Set the ImageReader property.
        setProperty(ImageReadDescriptor.PROPERTY_NAME_IMAGE_READER, reader);

        // If metadata are being read, set the value of the metadata
        // properties to UndefinedProperty so that the property
        // names will appear in the array of property names. The actual
        // values will be retrieved when getProperty() is invoked.
        if (!reader.isIgnoringMetadata()) {
            // Get the service provider interface, if any.
            ImageReaderSpi provider = reader.getOriginatingProvider();

            // Stream metadata.
            if (provider == null || provider.isStandardStreamMetadataFormatSupported()
                    || provider.getNativeStreamMetadataFormatName() != null) {
                // Assume an ImageReader with a null provider supports
                // stream metadata.
                setProperty(ImageReadDescriptor.PROPERTY_NAME_METADATA_STREAM,
                        java.awt.Image.UndefinedProperty);
            } else {
                // Provider supports neither standard nor native stream
                // metadata so set flag to suppress later reading attempt.
                streamMetadataRead = true;
            }

            // Image metadata.
            if (provider == null || provider.isStandardImageMetadataFormatSupported()
                    || provider.getNativeImageMetadataFormatName() != null) {
                // Assume an ImageReader with a null provider supports
                // image metadata.
                setProperty(ImageReadDescriptor.PROPERTY_NAME_METADATA_IMAGE,
                        java.awt.Image.UndefinedProperty);
            } else {
                // Provider supports neither standard nor native image
                // metadata so set flag to suppress later reading attempt.
                imageMetadataRead = true;
            }
        }
    }

    /**
     * Returns false as ImageReaders might return Rasters via computeTile() tFileImageDescriptorile that are internally
     * cached.
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * XXX
     */
    private Rectangle computeSourceRect( Rectangle destRect ) {
        Rectangle sourceRect = new Rectangle();

        sourceRect.x = scaleX * destRect.x + transX;
        sourceRect.y = scaleY * destRect.y + transY;

        sourceRect.width = scaleX * (destRect.x + destRect.width) + transX - sourceRect.x;
        sourceRect.height = scaleY * (destRect.y + destRect.height) + transY - sourceRect.y;

        return sourceRect;
    }

    /**
     * Computes a tile.
     * 
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster computeTile( int tileX, int tileY ) {
        // XXX System.out.println("Tile ("+tileX+","+tileY+")");
        // Create a new WritableRaster to represent this tile.
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        // WritableRaster dest = Raster.createWritableRaster(sampleModel, org);
        Rectangle rect = new Rectangle(org.x, org.y, tileWidth, tileHeight);

        // Clip output rectangle to image bounds.
        // Not sure what will happen here with the bounds intersection.
        Rectangle destRect = rect.intersection(getBounds());
        // XXX Check for destRect.isEmpty()?

        /*
         * XXX delete java.awt.geom.AffineTransform transform = new
         * java.awt.geom.AffineTransform(scaleX, 0, 0, scaleY, transX, transY);
         */
        Rectangle srcRect = computeSourceRect(destRect);
        /*
         * XXX delete transform.createTransformedShape(destRect).getBounds();
         */

        WritableRaster readerTile = null;
        try {
            synchronized (reader) {
                param.setSourceRegion(srcRect);
                BufferedImage bi = reader.read(imageIndex, param);
                WritableRaster ras = bi.getRaster();
                readerTile = ras.createWritableChild(0, 0, ras.getWidth(), ras.getHeight(), org.x,
                        org.y, null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WritableRaster tile = null;
        if (sampleModel == readerTile.getSampleModel()) {
            tile = readerTile;
        } else {
            // XXX As this method is synchronized, could a single
            // destination be supplied to the reader instead of
            // creating a new one?
            tile = Raster.createWritableRaster(sampleModel, org);
            tile.setRect(readerTile);
        }

        return tile;
    }

    /**
     * Throws an IllegalArgumentException since the image has no image sources.
     * 
     * @param sourceRect ignored.
     * @param sourceIndex ignored.
     * @throws IllegalArgumentException since the image has no sources.
     */
    public Rectangle mapSourceRect( Rectangle sourceRect, int sourceIndex ) {
        throw new IllegalArgumentException(Messages.getString("FileImageReadOpImage.mapSource"));
    }

    /**
     * Throws an IllegalArgumentException since the image has no image sources.
     * 
     * @param destRect ignored.
     * @param sourceIndex ignored.
     * @throws IllegalArgumentException since the image has no sources.
     */
    public Rectangle mapDestRect( Rectangle destRect, int sourceIndex ) {
        throw new IllegalArgumentException(Messages.getString("FileImageReadOpImage.mapDest"));
    }

}
