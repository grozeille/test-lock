delete from container;
delete from lambda;
delete from lambda_lock;
insert into lambda_lock (id, lastUpdateTimestamp) values ('pool', null);
