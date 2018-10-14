### DataSourceTransactionManager

PlatformTransactionManager

|-- AbstractPlatformTransactionManager 

|---- DataSourceTransactionManager

---

### 主要方法分析：

##### doGetTransaction

获取事务


	protected Object doGetTransaction() {
		// 创建一个事务对象
		DataSourceTransactionObject txObject = new DataSourceTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());
		// 以DataSource为key从管理事务的资源管理器获取事务
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

##### doBegin

开始事务
	
	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		Connection con = null;
	
		try {
			// 事务Object是新的, 所以这里要创建一个 ConnectionHolder
			if (!txObject.hasConnectionHolder() ||
					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Connection newCon = obtainDataSource().getConnection();
				if (logger.isDebugEnabled()) {
					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
				}
				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
			}
	
			// 同步属性? (待研究)
			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
			con = txObject.getConnectionHolder().getConnection();
			// 设置 readOnly 和 isolation 属性, 返回之前的isolation级别
			Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
			txObject.setPreviousIsolationLevel(previousIsolationLevel);
	
			// 原注释说的很清楚, 有些driver切换 commit 模式 耗费很大, 所以假如设置好了就不要设置了
			if (con.getAutoCommit()) {
				// (待研究)
				txObject.setMustRestoreAutoCommit(true);
				if (logger.isDebugEnabled()) {
					logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
				}
				con.setAutoCommit(false);
			}
			// 默认为通过sql设置 readOnly, 前提是设置了 enforceReadOnly
			prepareTransactionalConnection(con, definition);
			txObject.getConnectionHolder().setTransactionActive(true);
	
			// 获取timeout, 方法配置 > 全局配置
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
			}
	
			if (txObject.isNewConnectionHolder()) {
				TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
			}
		}
	
		catch (Throwable ex) {
			if (txObject.isNewConnectionHolder()) {
				DataSourceUtils.releaseConnection(con, obtainDataSource());
				txObject.setConnectionHolder(null, false);
			}
			throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
		}
	}