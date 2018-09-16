package com.jiange2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context.xml"})
public class TestOne {

	@Resource
	public DataSource dataSource;

	@Resource
	public Dao dao;

	@Test
	public void test(){
		System.out.println(dao.getClass());
		dao.insert();
	}


}
