package show.lmm.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import show.lmm.tenant.handler.TenantDataHandler;
import show.lmm.tenant.mapper.TestMapper;

import javax.annotation.Resource;

@SpringBootTest
class DemoApplicationTests {

    @Resource
    private TestMapper testMapper;

    @Test
    void testAdd(){
        TenantDataHandler.setTenantFieldValue(777L);
        System.out.println(testMapper.add("张三"));
    }

    @Test
    void testUpdate(){
        TenantDataHandler.setTenantFieldValue(777L);
        System.out.println(testMapper.update("张三@", 11));
    }

    @Test
    void testDelete(){
        TenantDataHandler.setTenantFieldValue(777L);
        System.out.println(testMapper.delete(11));
    }

    @Test
    void testSelect(){
        TenantDataHandler.setTenantFieldValue(777L);
        System.out.println(testMapper.list());
    }
}
