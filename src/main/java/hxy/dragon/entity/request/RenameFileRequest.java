package hxy.dragon.entity.request;

import lombok.Data;

/**
 * 文件重命名请求
 */
@Data
public class RenameFileRequest {
    /** 数据库自增ID，优先使用 */
    private Integer id;
    /** 兼容以UUID定位 */
    private String fileUuid;
    /** 新文件名（仅元数据，不修改物理文件） */
    private String newName;
}


