# ble_tool
> 蓝牙控制Led灯项目

## 通信协议说明
> 通信命令以"魔数"开头，后面两个字节为命令字以及数据长度。后面跟相应命令数据
> 每种命令的数据长度不固定，但是不可以超过255个字节。


#### 魔数
> YHZX


#### 协议头
> - 长度: 6个字节
> - 前四个字节:  魔数
> - 第五个字节: 命令字 (0 - 255)
> - 第六个字节: 数据长度 (0 - 255)



#### 命令字

```c
    enum {
        CMD_GET_BRIGHT = 30,
        CMD_SET_BRIGHT = 31,
        
        CMD_GET_DUTY = 32,
        CMD_SET_DUTY = 33,
        
        CMD_GET_BATTERY = 34,
        CMD_sET_BATTERY = 35,
        
        CMD_GET_TRIGGER = 36,
        CMD_SET_TRIGGER = 37
    }
```


#### 数据
> 每种命令字对应的协议数据结构不同

###### 例如：设置亮度的数据结构

```c
    struct {
        byte mode;
        byte bright_day;
        byte birght_night;
    }
```