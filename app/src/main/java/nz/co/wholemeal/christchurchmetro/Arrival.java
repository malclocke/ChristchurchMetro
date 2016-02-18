/**
 * Copyright 2010 Malcolm Locke
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.wholemeal.christchurchmetro;

import java.util.Calendar;
import java.util.Date;

class Arrival {

    public static final String TAG = "Arrival";

    public String routeNumber;
    public String routeName;
    public String destination;
    public String tripNumber;
    public int eta;
    public boolean wheelchairAccess;

    public Date getEstimatedArrivalTime() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.MINUTE, this.eta);
        return cal.getTime();
    }
}
