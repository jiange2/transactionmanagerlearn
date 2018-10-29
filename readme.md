# Spring事务管理

源码分析目录: [catagory](https://github.com/jiange2/transactionmanagerlearn/blob/master/note/catagory.md)

## 事务的属性

- 事务的隔离性

- 事务的传播性

- 事务的其他属性 (readOnly, timeOut, savePoint)

## 事务切面

### 关键类简介

- TransactionIntercepter

用于

### TransactionIntercepter

invoke:
``` java
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Work out the target class: may be {@code null}.
		// The TransactionAttributeSource should be passed the target class
		// as well as the method, which may be from an interface.
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

		// 执行事务
		return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
	}
```
invokeWithinTransaction：

这里省略了CallbackPreferringPlatformTransactionManager。只保留了PlatformTransactionManager的流程

	protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
			final InvocationCallback invocation) throws Throwable {

		// If the transaction attribute is null, the method is non-transactional.
		TransactionAttributeSource tas = getTransactionAttributeSource();
		final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		
		// Standard transaction demarcation with getTransaction and commit/rollback calls.
		TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
		Object retVal = null;
		try {
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			retVal = invocation.proceedWithInvocation();
		}
		catch (Throwable ex) {
			// target invocation exception
			completeTransactionAfterThrowing(txInfo, ex);
			throw ex;
		}
		finally {
			cleanupTransactionInfo(txInfo);
		}
		commitTransactionAfterReturning(txInfo);
		return retVal;
		
	}

