/*
 * Copyright (C) 2012 The LiquidSmooth Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.performance;

public class Voltage {
    private String freq;
    private String currentMv;
    private String savedMv;

    public void setFreq(final String freq) {
        this.freq = freq;
    }

    public String getFreq() {
        return freq;
    }

    public void setCurrentMV(final String currentMv) {
        this.currentMv = currentMv;
    }

    public String getCurrentMv() {
        return currentMv;
    }

    public void setSavedMV(final String savedMv) {
        this.savedMv = savedMv;
    }

    public String getSavedMV() {
        return savedMv;
    }
}
