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


public class AppInfo {

    private String domain;
    private android.content.pm.ApplicationInfo ApplicationInfo;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public android.content.pm.ApplicationInfo getApplicationInfo() {
        return ApplicationInfo;
    }

    public void setApplicationInfo(android.content.pm.ApplicationInfo applicationInfo) {
        ApplicationInfo = applicationInfo;
    }
}
