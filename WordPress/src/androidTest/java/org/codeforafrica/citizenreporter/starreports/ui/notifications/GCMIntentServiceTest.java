package org.codeforafrica.citizenreporter.starreports.ui.notifications;


import android.test.AndroidTestCase;

import org.codeforafrica.citizenreporter.starreports.GCMIntentService;

public class GCMIntentServiceTest extends AndroidTestCase {

    public void testShouldCircularizeNoteIcon() {
        GCMIntentService intentService = new GCMIntentService();

        String type = "c";
        assertTrue(intentService.shouldCircularizeNoteIcon(type));

        assertFalse(intentService.shouldCircularizeNoteIcon(null));

        type = "invalidType";
        assertFalse(intentService.shouldCircularizeNoteIcon(type));
    }
}
