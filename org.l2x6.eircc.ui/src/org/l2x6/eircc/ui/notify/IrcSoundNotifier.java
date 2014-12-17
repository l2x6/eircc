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

import org.l2x6.eircc.core.model.IrcMessage;
import org.l2x6.eircc.core.model.event.IrcModelEvent;
import org.l2x6.eircc.ui.EirccUi;
import org.l2x6.eircc.ui.prefs.IrcPreferences;

/**
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 */
public class IrcSoundNotifier {
    public enum SoundFile {
        ONE_KNOCK, TWO_KNOCKS, THREE_KNOCKS, WATER_DROP;

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
    private NowPlaying nowPlaying;

    /**
     *
     */
    public IrcSoundNotifier() {
        super();
    }

    public void handle(IrcModelEvent e) {
        try {
            switch (e.getEventType()) {
            case NICK_TIMEOUT:
                play(SoundFile.WATER_DROP);
                break;
            case NEW_MESSAGE:
                IrcMessage m = (IrcMessage) e.getModelObject();
                notify(m);
                break;
            default:
                break;
            }
        } catch (Exception e1) {
            EirccUi.log(e1);
        }

    }

    public void notify(IrcMessage m) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        if (IrcPreferences.getInstance().shouldPlaySoundForMessage(m)) {
            switch (m.getNotificationLevel()) {
            case NO_NOTIFICATION:
                break;
            case UNREAD_MESSAGES:
                play(SoundFile.ONE_KNOCK);
                break;
            case UNREAD_MESSAGES_FROM_A_TRACKED_USER:
                play(SoundFile.TWO_KNOCKS);
                break;
            case ME_NAMED:
                play(SoundFile.THREE_KNOCKS);
                break;
            default:
                break;
            }
        }
    }

    private static class NowPlaying {
        /**
         * @param endSystemTimeMs
         * @param soundFile
         */
        public NowPlaying(long endSystemTimeMs, SoundFile soundFile) {
            super();
            this.endSystemTimeMs = endSystemTimeMs;
            this.soundFile = soundFile;
        }
        private final long endSystemTimeMs;
        private final SoundFile soundFile;
        /**
         * @param soundFile2
         */
        public boolean accept(SoundFile soundFile) {
            return soundFile != this.soundFile || System.currentTimeMillis() > endSystemTimeMs;
        }
    }

    private void play(SoundFile soundFile) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        if (this.nowPlaying != null && !this.nowPlaying.accept(soundFile)) {
            /* we still play the same clip */
            return;
        }
        String path = soundFile.getAbsolutePath();
        URL url = this.getClass().getResource(path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
        Clip clip = AudioSystem.getClip();
        clip.removeLineListener(CLIP_AUTOCLOSE);
        clip.addLineListener(CLIP_AUTOCLOSE);
        clip.open(audioIn);
        long lengthMillis = clip.getMicrosecondLength() / 1000;
        this.nowPlaying = new NowPlaying(System.currentTimeMillis() + lengthMillis, soundFile);
        clip.start();
    }
}
