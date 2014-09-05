/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.l2x6.eircc.core.util.IrcUtils;
import org.l2x6.eircc.core.util.ReadableByteArrayOutputStream;
import org.l2x6.eircc.core.util.TypedField;
import org.l2x6.eircc.ui.EirccUi;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public interface PersistentIrcObject {
    TypedField[] getFields();

    IrcModel getModel();

    IPath getParentFolderPath();

    IPath getPath();

    default void load(IFile propsFile) throws IOException, CoreException {
        propsFile.refreshLocal(IResource.DEPTH_ZERO, null);
        try (Reader reader = new InputStreamReader(propsFile.getContents(), "utf-8")) {
            Properties props = new Properties();
            props.load(reader);
            for (TypedField field : this.getFields()) {
                String fieldName = field.name();
                String val = props.getProperty(fieldName);
                if (val != null) {
                    field.setString(this, val);
                }
            }
        }
    }

    default void save(IProgressMonitor monitor) throws CoreException {
        TypedField[] fields = getFields();
        if (fields.length > 0) {

            Properties props = new Properties();
            for (TypedField field : fields) {
                String fieldName = field.name();
                Object val = null;
                try {
                    val = field.getString(this);
                    if (val != null) {
                        props.put(fieldName, val.toString());
                    }
                } catch (Exception e) {
                    EirccUi.log("Could not save " + val + " to " + this.getClass().getSimpleName() + "." + fieldName);
                    EirccUi.log(e);
                }
            }
            IPath path = getPath();
            IFile file = getModel().getRoot().getFile(path);
            IPath backupPath = null;
            if (file.exists()) {
                backupPath = path.addFileExtension(".backup");
                file.copy(backupPath, true, monitor);
            } else {
                IrcUtils.mkdirs(file.getParent(), monitor);
            }

            ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
            try (Writer w = new OutputStreamWriter(out, "utf-8")) {
                props.store(w, "");
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, EirccUi.PLUGIN_ID, e.getClass().getName() + " "
                        + e.getMessage(), e));
            }
            InputStream in = out.createInputStream();
            if (!file.exists()) {
                file.create(in, true, monitor);
            } else {
                file.setContents(in, true, false, monitor);
            }

            if (backupPath != null) {
                IFile backupFile = getModel().getRoot().getFile(backupPath);
                if (backupFile.exists()) {
                    backupFile.delete(true, monitor);
                }
            }
        }
    }
}
