package su.erik.tabledataloader;

import org.apache.ibatis.annotations.Mapper;
import su.erik.tabledataloader.param.MapParam;
import java.util.List;
import java.util.Map;

@Mapper
public interface TestMapper {
    List<Map<String, Object>> testSelect(MapParam param);
    List<Map<String, Object>> selectChild(MapParam param);
}
