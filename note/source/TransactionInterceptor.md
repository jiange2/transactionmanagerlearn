## TransactionInterceptor

TransactionAspectSupport

|---TransactionInterceptor

因为Spring的事务是以AOP的形式切入的。所以需要有对MethodInterceptor的一个实现。这个实现就是TransactionInterceptor.

---

### 主要方法分析：

##### invoke

MethodInterceptor的入口就是invoke, 而这里的实现直接调用了父类(TransactionAspectSupport)的invokeWithinTransaction

    public Object invoke(MethodInvocation invocation) throws Throwable {
	    Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
        // 调用父类实现
        return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
	}

##### 构造方法 TransactionInterceptor
TransactionAttributeSource是切入点以及一些切入参数配置的封装。可以通过Properties配置或者直接注入TransactionAttributeSource对象。

    public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
		setTransactionManager(ptm);
		setTransactionAttributes(attributes);
	}

	public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
		setTransactionManager(ptm);
		setTransactionAttributeSource(tas);
	}

	// properties 注入
	<bean id="transactionInterceptor"
        class="org.springframework.transaction.interceptor.TransactionInterceptor">
        <property name="transactionManager" ref="transactionManager" />
        <!-- 配置事务属性 -->
        // 将会封装成 TransactionAttributeSource
        <property name="transactionAttributes">
            <props>
                <prop key="*">PROPAGATION_REQUIRED</prop>
            </props>
        </property>
    </bean>

    //直接注入transactionAttributeSource对象
    <bean id="transactionInterceptor" class="org.springframework.transaction.interceptor.TransactionInterceptor">
        <property name="transactionManager" ref="transactionManager"/>
        <property name="TransactionAttributeSource" ref="transactionAttributeSource"/>
    </bean>
    <bean id="transactionAttributeSource" class="org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource">
        <property name="properties">
            <props>
                <!-- PROPAGATION_, -->
                <prop key="*">PROPAGATION_REQUIRED</prop>
            </props>
        </property>
    </bean>

事务参数配置

spring事务的参数配置是以逗号分割每一项然后根据参数前缀判断参数配置的是哪一项。下面是前缀的常量定义

    // DefaultTransactionDefinition.class
    public static final String PREFIX_PROPAGATION = "PROPAGATION_";
    public static final String PREFIX_ISOLATION = "ISOLATION_";
    public static final String PREFIX_TIMEOUT = "timeout_";
    public static final String READ_ONLY_MARKER = "readOnly";
    // RuleBasedTransactionAttribute.class
    public static final String PREFIX_ROLLBACK_RULE = "-";
    public static final String PREFIX_COMMIT_RULE = "+";

example:

PROPAGATION_REQUIRED,ISOLATION_DEFAULT

PROPAGATION_REQUIRED,timeout_50

ISOLATION_DEFAULT,timeout_50,readOnly

ISOLATION_DEFAULT,-Exception.class,+RuntimeException

...



