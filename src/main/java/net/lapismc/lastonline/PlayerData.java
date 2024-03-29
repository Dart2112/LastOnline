/*
 * Copyright 2023 Benjamin Martin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lapismc.lastonline;

import java.util.Date;
import java.util.UUID;

public class PlayerData {

    private final UUID playerUUID;
    private Long time;

    public PlayerData(UUID uuid) {
        this(uuid, new Date().getTime());
    }

    public PlayerData(UUID uuid, Long time) {
        this.playerUUID = uuid;
        this.time = time;
    }

    public UUID getUUID() {
        return playerUUID;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
