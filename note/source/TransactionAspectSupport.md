## TransactionAspectSupport

TransactionAspectSupport

|---TransactionInterceptor

TransactionAspectSupport是事务切面(TransactionInterceptor)的父类,而事务切面主要的控制逻辑都在这个类实现了。

---

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
		// 获取当前事务管理器
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

---

##### determineTransactionManager

    protected PlatformTransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
        // Do not attempt to lookup tx manager if no tx attributes are set
        // 不是被切方法, 直接返回注入的 (这里其实返回null也是可以的)
        if (txAttr == null || this.beanFactory == null) {
            return getTransactionManager();
        }

        // qualifier, 是以类查找bean时的标识符
        String qualifier = txAttr.getQualifier();
        if (StringUtils.hasText(qualifier)) {
            // 根据 qualifier 查找 TransactionManager
            return determineQualifiedTransactionManager(this.beanFactory, qualifier);
        }
        else if (StringUtils.hasText(this.transactionManagerBeanName)) {
            // 假如有配置transactionManagerBeanName, 根据BeanName找
            return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
        }
        else {
            // 返回配置注入的 TransactionManager
            PlatformTransactionManager defaultTransactionManager = getTransactionManager();
            if (defaultTransactionManager == null) {
                // 去缓存中拿
                defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
                if (defaultTransactionManager == null) {
                    // 根据类找
                    defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
                    this.transactionManagerCache.putIfAbsent(
                            DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
                }
            }
            return defaultTransactionManager;
        }
	}

取 TransactionManager 逻辑: qualifier > transactionManagerBeanName > 直接注入 > 类

---

##### methodIdentification

    private String methodIdentification(Method method, @Nullable Class<?> targetClass,
			@Nullable TransactionAttribute txAttr) {

        // 使用子类重写的 methodIdentification
        String methodIdentification = methodIdentification(method, targetClass);
        if (methodIdentification == null) {
            // 配置 Propertities 参数, 初始化的是 RuleBasedTransactionAttribute (DefaultTransactionAttribute的子类)
            if (txAttr instanceof DefaultTransactionAttribute) {
                methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
            }
            // return 类名.方法名
            if (methodIdentification == null) {
                methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
            }
        }
        return methodIdentification;
	}

逻辑 : 子类重写 > DefaultTransactionAttribute 的 getDescriptor > 类名.方法名

---

##### createTransactionIfNecessary

    protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
			@Nullable TransactionAttribute txAttr, final String joinpointIdentification) {

        // If no name specified, apply method identification as transaction name.
        // 属性没有设置 name 属性的话 就把 methodIdentification 作为 name
        if (txAttr != null && txAttr.getName() == null) {
            txAttr = new DelegatingTransactionAttribute(txAttr) {
                @Override
                public String getName() {
                    return joinpointIdentification;
                }
            };
        }

        TransactionStatus status = null;
        if (txAttr != null) {
            if (tm != null) {
                // 调用事务管理器, 事务的 begin 会在这里进行
                status = tm.getTransaction(txAttr);
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
                            "] because no transaction manager has been configured");
                }
            }
        }
        // 创建事务info
        return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
	}

---
##### prepareTransactionInfo

    protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm,
            @Nullable TransactionAttribute txAttr, String joinpointIdentification,
            @Nullable TransactionStatus status) {

        TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
        if (txAttr != null) {
            // We need a transaction for this method...
            if (logger.isTraceEnabled()) {
                logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
            }
            // The transaction manager will flag an error if an incompatible tx already exists.
            txInfo.newTransactionStatus(status);
        }
        else {
            // The TransactionInfo.hasTransaction() method will return false. We created it only
            // to preserve the integrity of the ThreadLocal stack maintained in this class.
            if (logger.isTraceEnabled())
                logger.trace("Don't need to create transaction for [" + joinpointIdentification +
                        "]: This method isn't transactional.");
        }

        // We always bind the TransactionInfo to the thread, even if we didn't create
        // a new transaction here. This guarantees that the TransactionInfo stack
        // will be managed correctly even if no transaction was created by this aspect.
        txInfo.bindToThread();
        return txInfo;
	}