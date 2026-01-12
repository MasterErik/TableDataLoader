package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.param.HeaderUtils;
import su.erik.tabledataloader.param.MapParam;
import su.erik.tabledataloader.spi.MapParamProvider;

// Зависимости Spring допустимы, так как этот класс находится в src/test/java
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

public class MockMapParamProvider implements MapParamProvider {

    @Override
    public void fill(MapParam mapParam) {
        //Сначала устанавливаем данные, которые должны быть ВСЕГДА (Default Test User)
        mapParam.filter(StandardParam.USER_ID.getKey(), 999L);


        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // Если контекста нет (например, простой юнит-тест без мока запроса), просто выходим
        if (attrs == null) return;

        // Передаем адаптер (лямбду), который умеет доставать заголовок по имени
        HeaderUtils.fillMapParam(mapParam, attrs.getRequest()::getHeader);

        // 4. (Опционально) UserId и Roles из SecurityContext
        // Здесь можно добавить логику извлечения пользователя из Spring Security
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    }
}
