package hxy.dragon.dao.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hxy.dragon.dao.model.FileModel;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author eric
 * @program air-share
 * @description
 * @date 2022/3/12
 */
public interface FileMapper extends BaseMapper<FileModel> {

    default int saveUniqueByFileUuid(FileModel fileModel) {
        if (fileModel == null || !StringUtils.hasText(fileModel.getFileUuid())) {
            return insert(fileModel);
        }

        List<FileModel> existingRecords = selectList(new QueryWrapper<FileModel>()
                .eq("file_uuid", fileModel.getFileUuid())
                .orderByAsc("id"));
        if (existingRecords.isEmpty()) {
            try {
                return insert(fileModel);
            } catch (DuplicateKeyException e) {
                existingRecords = selectList(new QueryWrapper<FileModel>()
                        .eq("file_uuid", fileModel.getFileUuid())
                        .orderByAsc("id"));
            }
        }

        if (existingRecords.isEmpty()) {
            return 0;
        }

        FileModel firstRecord = existingRecords.getFirst();
        for (int i = 1; i < existingRecords.size(); i++) {
            delete(new QueryWrapper<FileModel>().eq("id", existingRecords.get(i).getId()));
        }

        fileModel.setId(firstRecord.getId());
        return update(fileModel, new UpdateWrapper<FileModel>().eq("id", firstRecord.getId()));
    }
}
