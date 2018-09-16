package com.jiange2;

import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.util.List;
import java.util.Map;

public class Dao extends JdbcDaoSupport {

	private Dao dao;

	@Transactional()
	public void insert(){
		dao.require();
	}

	public void require(){
		List<Map<String, Object>> list = getJdbcTemplate().queryForList("SELECT * FROM address");
		System.out.println(list);
	}

	public Dao getDao() {
		return dao;
	}

	public void setDao(Dao dao) {
		this.dao = dao;
	}
}
