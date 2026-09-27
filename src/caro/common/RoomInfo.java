package caro.common;

import java.io.Serializable;

/** Thông tin tóm tắt 1 phòng, dùng cho danh sách phòng đang chờ ở Lobby. */
public class RoomInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public String roomCode;
    public String hostName;
    public String status; // WAITING / PLAYING

    public RoomInfo() {
    }

    public RoomInfo(String roomCode, String hostName, String status) {
        this.roomCode = roomCode;
        this.hostName = hostName;
        this.status = status;
    }
}
