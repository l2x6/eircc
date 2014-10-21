/*
 * Copyright (c) 2014 Peter Palaga.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.l2x6.eircc.ui.notify;

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

import org.l2x6.eircc.core.model.IrcNotificationLevel;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSoundNotifier {
    public enum SoundFile {
        ME_NAMED, MESSAGE_FROM_TRACKED_USER;

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

    /**
     *
     */
    public IrcSoundNotifier() {
        super();
    }

    public void notify(IrcNotificationLevel level) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        switch (level) {
        case NO_NOTIFICATION:
            break;
        case UNREAD_MESSAGES:
            break;
        case UNREAD_MESSAGES_FROM_A_TRACKED_USER:
            play(SoundFile.MESSAGE_FROM_TRACKED_USER);
            break;
        case ME_NAMED:
            play(SoundFile.ME_NAMED);
            break;
        default:
            break;
        }
    }


    private void play(SoundFile soundFile) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        String path = soundFile.getAbsolutePath();
        URL url = this.getClass().getResource(path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
        Clip clip = AudioSystem.getClip();
        clip.removeLineListener(CLIP_AUTOCLOSE);
        clip.addLineListener(CLIP_AUTOCLOSE);
        clip.open(audioIn);
        clip.start();
    }
}
