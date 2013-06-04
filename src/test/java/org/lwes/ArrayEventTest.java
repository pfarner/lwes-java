/*======================================================================*
 * Licensed under the New BSD License (the "License"); you may not use  *
 * this file except in compliance with the License.  Unless required    *
 * by applicable law or agreed to in writing, software distributed      *
 * under the License is distributed on an "AS IS" BASIS, WITHOUT        *
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     *
 * See the License for the specific language governing permissions and  *
 * limitations under the License. See accompanying LICENSE file.        *
 *======================================================================*/
package org.lwes;

import org.junit.Test;

import junit.framework.Assert;

public final class ArrayEventTest extends EventTest {
    @Override
    protected ArrayEvent createEvent() {
        return new ArrayEvent();
    }
    
    @Test
    public void trim() {
        final ArrayEvent event = new ArrayEvent("Event");
        event.setInt32("x", 100);
        final ArrayEvent trimmedEvent = event.trim(10);
        Assert.assertEquals(event, trimmedEvent);
        trimmedEvent.setInt32("fits", 101);
    }
    
    @Test(expected=EventSystemException.class)
    public void trimOverrun() {
        final ArrayEvent event = new ArrayEvent("Event");
        event.setInt32("x", 100);
        final ArrayEvent trimmedEvent = event.trim(0);
        trimmedEvent.setInt32("overrun", 100);
    }
}
