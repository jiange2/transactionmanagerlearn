## TransactionAspectSupport

TransactionAspectSupport

|---TransactionInterceptor

TransactionAspectSupport是事务切面(TransactionInterceptor)的父类,而事务切面主要的控制逻辑都在这个类实现了。

### 主要方法分析：

##### invokeWithinTransaction

这个方法是事务的主要控制流程, 在TransactionInterceptor的invoke直接调用的就是这个方法。

	protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
			final InvocationCallback invocation) throws Throwable {

		// If the transaction attribute is null, the method is non-transactional.
		// 获取方法的事务配置信息 (前面提到过,TransactionAttributeSource是配置信息的封装类)
		TransactionAttributeSource tas = getTransactionAttributeSource();
		// TransactionAttribute 是配置信息的每一项. (每个方法都要相应的配置信息)
		final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
		// 获取当前事务管理器, determineTransactionManager 有一些获取管理器的逻辑
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		// 获取事务标识符, 假如子类没有重写, 这个标识符就是 类名.方法名
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
			// Standard transaction demarcation with getTransaction and commit/rollback calls.
			// createTransactionIfNecessary 这个方法主要是封装了一些事务相关的信息以及启动事务
			TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
			Object retVal = null;
			try {
				// This is an around advice: Invoke the next interceptor in the chain.
				// This will normally result in a target object being invoked.
				// 执行业务代码
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				// target invocation exception
				// 异常处理 (待研究)
				completeTransactionAfterThrowing(txInfo, ex);
				throw ex;
			}
			finally {
			    // 清理信息 (待研究)
				cleanupTransactionInfo(txInfo);
			}
			// 提交 (待研究)
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}
		else { // 事务管理器是 CallbackPreferringPlatformTransactionManager
			final ThrowableHolder throwableHolder = new ThrowableHolder();

			// It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
			try {
				Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr, status -> {
					TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
					try {
						return invocation.proceedWithInvocation();
					}
					catch (Throwable ex) {
						if (txAttr.rollbackOn(ex)) {
							// A RuntimeException: will lead to a rollback.
							if (ex instanceof RuntimeException) {
								throw (RuntimeException) ex;
							}
							else {
								throw new ThrowableHolderException(ex);
							}
						}
						else {
							// A normal return value: will lead to a commit.
							throwableHolder.throwable = ex;
							return null;
						}
					}
					finally {
						cleanupTransactionInfo(txInfo);
					}
				});

				// Check result state: It might indicate a Throwable to rethrow.
				if (throwableHolder.throwable != null) {
					throw throwableHolder.throwable;
				}
				return result;
			}
			catch (ThrowableHolderException ex) {
				throw ex.getCause();
			}
			catch (TransactionSystemException ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
					ex2.initApplicationException(throwableHolder.throwable);
				}
				throw ex2;
			}
			catch (Throwable ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
				}
				throw ex2;
			}
		}
	}

方法比较大。但在大概了解几个方法的功能之后并不难理解, 总体和事务的代码基本一样, 只是Spring需要做的很周全。

经典事务代码：

    try{
        startTransaction; // createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
        // 这里在Spring 需要 考虑各种 事务嵌套的情况
        ...
    }catch{
        rollback; // 这里spring不一定回滚, 根据配置决定
    } finally{
        clear; // 这里Spring需要做一些清理工作，比如在事务嵌套的情况下，Spring需要把当前事务恢复到上一层事务
    }
    commit;
