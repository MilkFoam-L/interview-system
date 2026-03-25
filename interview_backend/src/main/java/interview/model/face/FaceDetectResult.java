package interview.model.face;

import lombok.Data;

/**
 * 人脸检测响应数据模型
 */
@Data
public class FaceDetectResult {
    private Header header;
    private Payload payload;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Data
    public static class Header {
        private int code;
        private String message;
        private String sid;
        
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSid() {
            return sid;
        }
        
        public void setSid(String sid) {
            this.sid = sid;
        }
    }

    @Data
    public static class Payload {
        private FaceResult face_detect_result;
        
        public FaceResult getFace_detect_result() {
            return face_detect_result;
        }
        
        public void setFace_detect_result(FaceResult face_detect_result) {
            this.face_detect_result = face_detect_result;
        }
    }

    @Data
    public static class FaceResult {
        private String compress;
        private String encoding;
        private String format;
        private String text;
        
        public String getCompress() {
            return compress;
        }
        
        public void setCompress(String compress) {
            this.compress = compress;
        }
        
        public String getEncoding() {
            return encoding;
        }
        
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }
        
        public String getFormat() {
            return format;
        }
        
        public void setFormat(String format) {
            this.format = format;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
} 