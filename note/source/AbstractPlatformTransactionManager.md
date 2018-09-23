## PlatformTransactionManager

PlatformTransactionManager

|-- AbstractPlatformTransactionManager 

|---- DataSourceTransactionManager

---

### 主要方法分析：

##### getTransaction

	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
		// 获取一个包含所有事务信息的对象(假如已有事务的话也会取出来)
		Object transaction = doGetTransaction();
	
		// Cache debug flag to avoid repeated checks.
		boolean debugEnabled = logger.isDebugEnabled();
	
		// TransactionAspectSupport 的实现不会传入null
		if (definition == null) {
			// Use defaults if no transaction definition given.
			// 使用默认属性
			definition = new DefaultTransactionDefinition();
		}
	
		// 默认返回 false
		// DataSourceTransactionManager
		// 根据 TransactionObject 判断是否有 ConnectionHolder 且是否处于 active 状态 (active待研究)
		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			// 根据 事务传播属性 进行各种操作
			return handleExistingTransaction(definition, transaction, debugEnabled);
		}
	
		// Check definition settings for new transaction.
		// 配置的事务超时时间小于 -1 抛出异常
		if (definition.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", definition.getTimeout());
		}
	
		// No existing transaction found -> check propagation behavior to find out how to proceed.
		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		// 三种需要事务的 传播属性 在当前没有事务的情况下创建新事务
		else if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			// 挂起事务? (待研究)
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
			}
			try {
				// 同步?(待研究)
				boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
				// 创建一个status (待研究)
				DefaultTransactionStatus status = newTransactionStatus(
						definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
				// 开始事务
				doBegin(transaction, definition);
				// 设置当前事务的各种属性
				prepareSynchronization(status, definition);
				return status;
			}
			catch (RuntimeException | Error ex) {
				resume(null, suspendedResources);
				throw ex;
			}
		}
		else {
			// Create "empty" transaction: no actual transaction, but potentially synchronization.
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
						"isolation level will effectively be ignored: " + definition);
			}
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(definition, null, true, newSynchronization, debugEnabled, null);
		}
	}