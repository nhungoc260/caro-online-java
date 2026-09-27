package caro.server;

import caro.common.Message;
import caro.common.User;
import caro.database.DatabaseConnection;
import caro.database.UserDAO;

/**
 * Business logic cho đăng nhập / đăng ký.
 * Đây là nơi DUY NHẤT chuyển đổi giữa "Message" (giao thức mạng) và
 * "UserDAO" (tầng dữ liệu) - AuthServer/ClientHandler không gọi UserDAO
 * trực tiếp, luôn đi qua AuthService để business rule tập trung 1 chỗ.
 *
 * Toàn bộ validate ở đây là kiểm tra PHÍA SERVER (không tin dữ liệu client
 * gửi lên, dù RegisterFrame/LoginFrame đã validate ở client rồi).
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Xử lý 1 request LOGIN, trả về Message sẵn sàng gửi ngược lại client:
     * LOGIN_SUCCESS (kèm user) hoặc LOGIN_FAILED (kèm note = lý do).
     */
    public Message login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return failed(Message.Type.LOGIN_FAILED, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }
        try {
            User user = userDAO.authenticate(username.trim(), password);
            if (user == null) {
                return failed(Message.Type.LOGIN_FAILED, "Sai tên đăng nhập hoặc mật khẩu.");
            }
            userDAO.updateStatus(user.getId(), "ONLINE");
            Message ok = new Message(Message.Type.LOGIN_SUCCESS);
            ok.user = user;
            return ok;
        } catch (DatabaseConnection.DatabaseException e) {
            return failed(Message.Type.LOGIN_FAILED, e.getMessage());
        }
    }

    /**
     * Xử lý 1 request REGISTER, trả về Message sẵn sàng gửi ngược lại client:
     * REGISTER_SUCCESS (kèm user) hoặc REGISTER_FAILED (kèm note = lý do).
     */
    public Message register(String username, String password, String displayName) {
        String validationError = validateRegister(username, password, displayName);
        if (validationError != null) {
            return failed(Message.Type.REGISTER_FAILED, validationError);
        }
        try {
            String finalDisplayName = isBlank(displayName) ? username.trim() : displayName.trim();
            User user = userDAO.register(username.trim(), password, finalDisplayName);
            Message ok = new Message(Message.Type.REGISTER_SUCCESS);
            ok.user = user;
            return ok;
        } catch (DatabaseConnection.DatabaseException e) {
            return failed(Message.Type.REGISTER_FAILED, e.getMessage());
        }
    }

    /**
     * Validate phía server - PHẢI kiểm tra lại dù client đã kiểm tra rồi,
     * vì client có thể bị sửa đổi / bị bypass (theo nguyên tắc "không tin
     * dữ liệu từ Client" - mục XXV của yêu cầu).
     *
     * @return null nếu hợp lệ, hoặc chuỗi mô tả lỗi đầu tiên gặp phải
     */
    private String validateRegister(String username, String password, String displayName) {
        if (isBlank(username) || isBlank(password)) {
            return "Vui lòng nhập đầy đủ thông tin bắt buộc.";
        }
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < 3) {
            return "Tên đăng nhập phải có ít nhất 3 ký tự.";
        }
        if (trimmedUsername.length() > 32) {
            return "Tên đăng nhập không được vượt quá 32 ký tự.";
        }
        if (!trimmedUsername.matches("[a-zA-Z0-9_]+")) {
            return "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (displayName != null && displayName.trim().length() > 64) {
            return "Tên hiển thị không được vượt quá 64 ký tự.";
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Message failed(Message.Type type, String reason) {
        Message m = new Message(type);
        m.note = reason;
        return m;
    }
}
