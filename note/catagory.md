## 类分析

##### [TransactionInterceptor](https://github.com/jiange2/transactionmanagerlearn/blob/master/note/source/TransactionInterceptor.md)

因为Spring的事务是以AOP的形式切入的。所以需要有对MethodInterceptor的一个实现。这个实现就是TransactionInterceptor.

##### [TransactionAspectSupport](https://github.com/jiange2/transactionmanagerlearn/blob/master/note/source/TransactionAspectSupport.md)

TransactionAspectSupport是事务切面(TransactionInterceptor)的父类,而事务切面主要的控制逻辑都在这个类实现了。
