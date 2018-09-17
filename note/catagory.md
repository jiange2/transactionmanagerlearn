## 类分析

##### TransactionInterceptor

因为Spring的事务是以AOP的形式切入的。所以需要有对MethodInterceptor的一个实现。这个实现就是TransactionInterceptor.

源码分析: 

##### TransactionAspectSupport

