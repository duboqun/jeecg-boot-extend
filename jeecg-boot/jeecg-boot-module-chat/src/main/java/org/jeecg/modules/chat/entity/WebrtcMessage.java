package org.jeecg.modules.chat.entity;

/**
 * ws接受webrtc消息类
 *
 * @author dongjb
 * @date 2020/05/16
 * @since 1.0.0
 */
public class WebrtcMessage {
    public static final String TYPE_COMMAND_ROOM_ENTER = "enterRoom";
    public static final String TYPE_COMMAND_ROOM_CREATE = "createRoom";
    public static final String TYPE_COMMAND_READY = "ready";
    public static final String TYPE_COMMAND_OFFER = "offer";
    public static final String TYPE_COMMAND_ANSWER = "answer";
    public static final String TYPE_COMMAND_CANDIDATE = "candidate";


    public static final String TYPE_COMMAND_ERROR = "error";
    public static final String TYPE_COMMAND_SUCCESS = "success";
    public static final String TYPE_COMMAND_CHAT = "chat";
    public static final String TYPE_COMMAND_MUTED = "MUTED";
    public static final String TYPE_COMMAND_VIEW = "VIEW";
    public static final String TYPE_COMMAND_BAN = "BAN";
    public static final String TYPE_COMMAND_KICK = "KICK";
    public static final String TYPE_COMMAND_SIGN = "SIGN";

    private String command;
    private String userId;
    private String roomId;
    private String message;
    private String roomPw;
    private String token;

    public WebrtcMessage() {

    }

    public WebrtcMessage(String command, String userId, String roomId, String message) {
        this.command = command;
        this.userId = userId;
        this.roomId = roomId;
        this.message = message;
    }

    public WebrtcMessage(String command, String userId, String message) {
        this.command = command;
        this.userId = userId;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRoomPw() {
        return roomPw;
    }

    public void setRoomPw(String roomPw) {
        this.roomPw = roomPw;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "WebrtcMessage{" +
                "command='" + command + '\'' +
                ", userId='" + userId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", message='" + message + '\'' +
                ", roomPw='" + roomPw + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
