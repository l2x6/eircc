/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.l2x6.eircc.core.IrcModelEvent;
import org.l2x6.eircc.core.IrcModelEventListener;
import org.l2x6.eircc.core.model.IrcLog;
import org.l2x6.eircc.core.model.IrcLog.LogState;
import org.l2x6.eircc.core.model.IrcModel;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSoundNotifier implements IrcModelEventListener {
    public enum SoundFile {
        NOTIFICATION;

        private final String absolutePath;

        private SoundFile() {
            this.absolutePath = "/sounds/" + name().toLowerCase(Locale.ENGLISH) + ".wav";
        }

        public String getAbsolutePath() {
            return absolutePath;
        }
    }

    private static final LineListener CLIP_AUTOCLOSE = new LineListener() {
        @Override
        public void update(LineEvent event) {
            if (event.getType().equals(LineEvent.Type.STOP)) {
                Line soundClip = event.getLine();
                soundClip.close();
            }
        }
    };

    private static final IrcSoundNotifier INSTANCE = new IrcSoundNotifier();;

    public static IrcSoundNotifier getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    public IrcSoundNotifier() {
        super();
        IrcModel.getInstance().addModelEventListener(this);
    }

    public void dispose() {
        IrcModel.getInstance().removeModelEventListener(this);
    }

    /**
     * @see org.l2x6.eircc.core.IrcModelEventListener#handle(org.l2x6.eircc.core.IrcModelEvent)
     */
    @Override
    public void handle(IrcModelEvent e) {
        try {
            switch (e.getEventType()) {
            case LOG_STATE_CHANGED:
                IrcLog log = (IrcLog) e.getModelObject();
                if (log.getState() == LogState.ME_NAMED) {
                    meNamed();
                }
                break;
            default:
                break;
            }
        } catch (Exception e1) {
            EirccUi.log(e1);
        }
    }

    private void play(SoundFile soundFile) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        String path = SoundFile.NOTIFICATION.getAbsolutePath();
        URL url = this.getClass().getResource(path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
        Clip clip = AudioSystem.getClip();
        clip.removeLineListener(CLIP_AUTOCLOSE);
        clip.addLineListener(CLIP_AUTOCLOSE);
        clip.open(audioIn);
        clip.start();
    }

    public void meNamed() throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        play(SoundFile.NOTIFICATION);
    }
}
