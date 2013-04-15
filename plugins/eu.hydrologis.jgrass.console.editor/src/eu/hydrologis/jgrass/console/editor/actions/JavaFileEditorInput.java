/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * (C) C.U.D.A.M. Universita' di Trento
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
  package eu.hydrologis.jgrass.console.editor.actions;

/**



 * Copyright (c) 2000, 2005 IBM Corporation and others.



 * All rights reserved. This program and the accompanying materials



 * are made available under the terms of the Eclipse Public License v1.0



 * which accompanies this distribution, and is available at



 * http://www.eclipse.org/legal/epl-v10.html



 *



 * Contributors:



 *     IBM Corporation - initial API and implementation



 */

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * 
 * 
 * @since 3.0
 * 
 * 
 */

public class JavaFileEditorInput implements IPathEditorInput, ILocationProvider {

	public final static String UNTITLEDFILE = System.getProperty("user.home")
			+ File.separator + "default.jgrass";

	/**
	 * 
	 * 
	 * The workbench adapter which simply provides the label.
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @since 3.1
	 * 
	 * 
	 */

	private class WorkbenchAdapter implements IWorkbenchAdapter {

		private WorkbenchAdapter() {

			super();

		}

		public Object[] getChildren(Object o) {

			return null;

		}

		public ImageDescriptor getImageDescriptor(Object object) {

			return null;
		}

		public String getLabel(Object o) {

			return ((JavaFileEditorInput) o).getName();

		}

		public Object getParent(Object o) {

			return null;

		}

	}

	private File fFile;

	private WorkbenchAdapter fWorkbenchAdapter = new WorkbenchAdapter();

	public JavaFileEditorInput(File file) {

		super();

		fFile = file;

		fWorkbenchAdapter = new WorkbenchAdapter();

	}

	public boolean exists() {

		return fFile.exists();

	}

	public ImageDescriptor getImageDescriptor() {

		return null;

	}

	public String getName() {

		return fFile.getName();

	}

	public IPersistableElement getPersistable() {

		return null;

	}

	public String getToolTipText() {

		return fFile.getAbsolutePath();

	}

	public String getAbsolutePath() {

		return fFile.getAbsolutePath();

	}

	public Object getAdapter(Class adapter) {

		if (ILocationProvider.class.equals(adapter))
			return this;

		if (IWorkbenchAdapter.class.equals(adapter))
			return fWorkbenchAdapter;

		return Platform.getAdapterManager().getAdapter(this, adapter);

	}

	public IPath getPath(Object element) {

		if (element instanceof JavaFileEditorInput) {

			JavaFileEditorInput input = (JavaFileEditorInput) element;

			return Path.fromOSString(input.fFile.getAbsolutePath());

		}

		return null;

	}

	public IPath getPath() {

		return Path.fromOSString(fFile.getAbsolutePath());

	}

	public boolean equals(Object o) {

		if (o == this)
			return true;

		if (o instanceof JavaFileEditorInput) {

			JavaFileEditorInput input = (JavaFileEditorInput) o;

			return fFile.equals(input.fFile);

		}

		if (o instanceof IPathEditorInput) {

			IPathEditorInput input = (IPathEditorInput) o;

			return getPath().equals(input.getPath());

		}

		return false;

	}

	public int hashCode() {

		return fFile.hashCode();

	}

}
