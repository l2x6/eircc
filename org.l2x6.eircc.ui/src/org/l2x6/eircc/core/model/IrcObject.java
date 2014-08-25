/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Properties;

import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.l2x6.eircc.ui.EirccUi;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public abstract class IrcObject {
    protected File saveDirectory;

    /**
     * @param parentDir
     */
    protected IrcObject() {
    }

    protected IrcObject(File parentDir) {
        super();
        this.saveDirectory = parentDir;
    }

    public abstract void dispose();

    public abstract Enum<?>[] getFields();

    File getSaveDirectory() {
        return saveDirectory;
    }

    /**
     * @return
     */
    protected abstract File getSaveFile();

    /**
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     *
     */
    protected void load(File propsFile) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(propsFile), "utf-8")) {
            Properties props = new Properties();
            props.load(reader);
            for (Enum<?> field : this.getFields()) {
                String fieldName = field.name();
                String val = props.getProperty(fieldName);
                if (val != null) {
                    IObservableValue observer = PojoProperties.value(this.getClass(), fieldName).observe(this);
                    Object typedVal = ((TypedField) field).fromString(val);
                    observer.setValue(typedVal);
                }
            }
        }
    }

    /**
     * @param accountsDir
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void save() throws UnsupportedEncodingException, FileNotFoundException, IOException {
        File parentDir = getSaveDirectory();
        Enum<?>[] fields = getFields();
        if (fields.length > 0) {
            parentDir.mkdirs();
            Properties props = new Properties();
            for (Enum<?> field : fields) {
                String fieldName = field.name();
                Object val = null;
                try {
                    IObservableValue observer = PojoProperties.value(this.getClass(), fieldName).observe(this);
                    val = observer.getValue();
                    if (val != null) {
                        props.put(fieldName, val.toString());
                    }
                } catch (Exception e) {
                    EirccUi.log("Could not save " + val + " to " + this.getClass().getSimpleName() + "." + fieldName);
                    EirccUi.log(e);
                }
            }
            File file = getSaveFile();
            File backupFile = null;
            if (file.exists()) {
                backupFile = new File(file.getParent(), file.getName() + ".backup");
                file.renameTo(backupFile);
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
                props.store(w, "");
            }
            if (backupFile != null && backupFile.exists()) {
                backupFile.delete();
            }
        }
    }
}
