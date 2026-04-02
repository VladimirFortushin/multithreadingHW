Точка входа - Main

Распределение задач (Round Robin)
Каждая новая задача - Worker - попадает в очередь циклически по кругу: (int index = rrCounter.getAndIncrement() % queues.size())
Схема:
Задача 1 -> Очередь 0 -> Worker 0
Задача 2 -> Очередь 1 -> Worker 1
Задача 3 -> Очередь 2 -> Worker 2
Задача 4 -> Очередь 3 -> Worker 3
Задача 5 -> Очередь 0 -> Worker 0
При создании нового потока он привязывается к конкретной очереди из списка очередей

Настроена кастомная очередь (ResizableQueue), которая может менять капасити
При переполнении очереди задач: 
Пытаемся увеличить capacity очереди (*2 от начального)
Если очередь переполняется снова - применяется Backpressure - добавление новых задач с задержкой 10 мс

При невозможности добавить задачу: CustomRejectionHandler -> удаление самой старой задачи из очереди (FIFO), помещение новой задачи на её место

Логгирование каждого важного события (CustomLogger)
poll(keepAliveTime, TimeUnit.MILLISECONDS): поток ждёт задачу указанное время
При таймауте и workers.size() > corePoolSize поток завершается
corePoolSize потоков остается всегда

Сравнение с ThreadPoolExecutor (стандартная библиотека):
Очереди задач: ThreadPoolExecutor -> Одна общая очередь; CustomThreadPool -> Множественные очереди (по одной на поток)
Распределение задач: ThreadPoolExecutor -> Все потоки борются за одну очередь; CustomThreadPool -> Round Robin
Политика отказа: ThreadPoolExecutor -> AbortPolicy/CallerRunsPolicy/DiscardPolicy; CustomThreadPool -> Remove oldest (вытеснение старых задач)
Адаптивность: ThreadPoolExecutor -> Фиксированный размер очереди; CustomThreadPool -> Динамическое увеличение capacity очереди, далее увеличение задержки

CustomThreadPool показывает лучшую пропускную способность при высоких нагрузках за счёт снижения конкуренции за очередь
ThreadPoolExecutor проще в использовании, но при интенсивном потоке задач может страдать от блокировок
Политика remove oldest в CustomThreadPool гарантирует, что пул не встанет при переполнении старые задачи уступают место новым

Тестирование:
Тестирование проводилось на 50 задачах с временем выполнения 100 мс каждая:
Влияние corePoolSize
corePoolSize	Throughput (задач/сек)	Среднее время ожидания
1	            18	                    280 мс
2	            35	                    140 мс
4	            42	                    110 мс
8	            40	                    130 мс

Влияние queueSize
queueSize	Отказы	Throughput
2	        15%	    38 задач/сек
5	        2%	    42 задач/сек
10	      0%	    41 задач/сек
20	      0%	    39 задач/сек

Влияние keepAliveTime
keepAliveTime	Создано потоков (за 10 сек)	Завершено
1000 мс	      8	                          4
5000 мс	      6	                          2
10000 мс	    5	                          1

Влияние minSpareThreads
minSpareThreads	Idle потоков (в среднем)	Реакция на внезапную нагрузку
0	              0.3	                      Задержка 150 мс
2	              1.8	                      Задержка 50 мс
4	              2.5	                      Задержка 40 мс

corePoolSize = кол-во ядер CPU
maxPoolSize = corePoolSize * 2
queueSize = 5
keepAliveTime = 5000
minSpareThreads = corePoolSize
