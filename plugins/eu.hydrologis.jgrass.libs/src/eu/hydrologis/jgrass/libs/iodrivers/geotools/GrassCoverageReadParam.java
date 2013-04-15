/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.jgrass.libs.iodrivers.geotools;

import eu.hydrologis.jgrass.libs.iodrivers.JGrassMapEnvironment;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;

/**
 * Represents the read parameters in the geotools space, as opposed to
 * {@linkplain GrassBinaryImageReadParam} that are for the imageio space.
 * <p>
 * Represents the parameters needed by the {@linkplain GrassCoverageReader} for reading coverage,
 * i.e. the portion and resolution of the map you want to get from the GridCoverageReader.
 * </p>
 * <p>
 * The needed parameters to read a GRASS raster map, are the following:
 * <ul>
 * <li>the northern boundary coordinate</li>
 * <li>the southern boundary coordinate</li>
 * <li>the eastern boundary coordinate</li>
 * <li>the western boundary coordinate</li>
 * <li>the north-south or west-east resolution</li>
 * <li>the number of rows and columns</li>
 * </ul>
 * </p>
 * <p>
 * All these values are already handled in the {@linkplain JGrassRegion}, so that has to be supplied
 * in order to choose a region different from the native data region.
 * </p>
 * <b>Note:</b> it is enough to have bounds and row-cols, or bounds and resolutions, or also a
 * corner and row-cols and resolutions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see {@link JGrassRegion}
 * @see {@link JGrassMapEnvironment}
 */
public class GrassCoverageReadParam {

    /**
     * The active read region used by the {@linkplain GrassCoverageReader} for defining the
     * requested map portion.
     */
    private JGrassRegion requestedWorldRegion = null;

    /**
     * Constructs a {@link GrassCoverageReadParam}.
     * 
     * @param requestedWorldRegion the active region to which to set read region to.
     */
    public GrassCoverageReadParam( JGrassRegion requestedWorldRegion ) {
        this.requestedWorldRegion = requestedWorldRegion;
    }

    /**
     * Getter for the {@linkplain GrassCoverageReadParam#requestedWorldRegion active region}
     * 
     * @param activeRegion the active region. If this is null, the whole raster map region should be
     *        used.
     */
    public JGrassRegion getRequestedWorldRegion() {
        return requestedWorldRegion;
    }

}
