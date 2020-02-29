package app.fedilab.nitterizeme;

/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of NitterizeMe
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * NitterizeMe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with NitterizeMe; if not,
 * see <http://www.gnu.org/licenses>. */
class Instance {

    private String domain;
    private long latency = -1;
    private boolean checked = false;
    private instanceType type;
    private boolean cloudflare =false;
    private String locale;

    String getDomain() {
        return domain;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    long getLatency() {
        return latency;
    }

    void setLatency(long latency) {
        this.latency = latency;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean checked) {
        this.checked = checked;
    }

    instanceType getType() {
        return type;
    }

    void setType(instanceType type) {
        this.type = type;
    }

    public boolean isCloudflare() {
        return cloudflare;
    }

    public void setCloudflare(boolean cloudflare) {
        this.cloudflare = cloudflare;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    enum instanceType {
        INVIDIOUS,
        NITTER,
        BIBLIOGRAM
    }
}
