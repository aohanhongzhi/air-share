package hxy.dragon.entity.enums;


/**
 * @author houxiaoyi
 */

public enum FileTypeEnum implements Enumerator {

    DOC("doc", 1),
    DOCX("docx", 2),
    PPT("ppt", 3),
    PPTX("pptx", 4),
    PDF("pdf", 5),
    UNKNOW("unknow", 0);

    String type;

    Integer code;

    FileTypeEnum(String type, Integer code) {
        this.type = type;
        this.code = code;
    }

    public String getType() {
        return this.type;
    }

    public Integer getCode() {
        return this.code;
    }

    public static Integer getCodeByType(String type) {
        for (FileTypeEnum fileTypeEnum : FileTypeEnum.values()) {
            if (fileTypeEnum.getType().equals(type)) {
                return fileTypeEnum.getCode();
            }
        }
        return UNKNOW.getCode();
    }

    public static FileTypeEnum getEnumByType(String type) {
        for (FileTypeEnum fileTypeEnum : FileTypeEnum.values()) {
            if (fileTypeEnum.getType().equals(type)) {
                return fileTypeEnum;
            }
        }
        return UNKNOW;
    }


    @Override
    public Integer code() {
        return this.code;
    }

    @Override
    public String description() {
        return this.type;
    }
}
