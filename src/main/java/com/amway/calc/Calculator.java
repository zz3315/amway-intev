package com.amway.calc;

import lombok.Getter;

import java.util.Objects;

public class Calculator {
    /**
     * 计算器整个操作过程及中间结果到保存在改变量中
     * 该变量数据解构定义为单链表（指向前驱）-方便undo操作，当前的操作（加减乘除）-方便redo操作
     * 整个计算器的核心实现就是保存在这个对象中，CRUD的操作，全部转换成对应的对单链表的操作
     */
    private volatile ValueDto valueDto;

    /**
     * 计算器当前的结果
     *
     * @return
     */
    public int getResult() {
        return valueDto.getResult();
    }

    /**
     * 初始化计算器
     *
     * @return
     */
    public static Calculator build() {
        return new Calculator();
    }

    /**
     * 屏蔽外部构造，只能使用build来构造
     */
    private Calculator() {
    }

    /**
     * 加法
     *
     * @param num 加数
     * @return 加之后的结果
     */
    public synchronized int add(int num) {
        return exec(OperationEnum.ADD, num);
    }

    /**
     * 加法
     *
     * @param num 减数
     * @return 减之后的结果
     */
    public synchronized int sub(int num) {
        return exec(OperationEnum.SUB, num);
    }

    /**
     * 乘法
     *
     * @param num 乘数
     * @return 乘之后的结果
     */
    public synchronized int multi(int num) {
        return exec(OperationEnum.MULTI, num);
    }

    /**
     * 除法
     *
     * @param num 被除数
     * @return 除之后的结果
     */
    public synchronized int devi(int num) {
        if (num == 0) {
            //除数为0
            throw new UnsupportedOperationException();
        }
        return exec(OperationEnum.DEVI, num);
    }

    /**
     * 撤销，可以撤销到初始状态-0
     * 思想：找单链表的前驱的结果值返回，同时把当前节点删除，当前驱为null时，即已经到父节点，直接返回0
     *
     * @return 撤销后的结果
     */
    public synchronized int undo() {
        if (Objects.isNull(valueDto)) {
            // 不存在值的情况
            return 0;
        }

        //没有上一步，直接返回当前的值,否则返回上一步的结果值
        int res = Objects.isNull(valueDto.getPre()) ? 0 : valueDto.getPre().getResult();

        //需要删除当前的节点
        valueDto = valueDto.getPre();
        return res;
    }

    /**
     * 重做
     *
     * @return 重做后的结果
     */
    public synchronized int redo() {
        if (Objects.isNull(valueDto)) {
            // 不存在值的情况
            return 0;
        }
        exec(valueDto.getOperation(), valueDto.getCurrentNum());
        return getResult();
    }

    /**
     * 首次初始化值
     *
     * @param operationEnum 操作类型
     * @param currentNum    当前值
     */
    private void init(OperationEnum operationEnum, int currentNum) {
        valueDto = ValueDto.builder()
                .currentNum(currentNum)
                .currentOperation(operationEnum)
                //都初始化为当前值
                .result(currentNum)
                .pre(null)
                .build();
    }

    /**
     * 开始计算
     *
     * @param operationEnum 操作
     * @param currentNum    当前值
     * @return 当前计算器的结果
     */
    private int exec(OperationEnum operationEnum, int currentNum) {
        if (Objects.isNull(valueDto)) {
            init(operationEnum, currentNum);
        } else {
            handlerCalc(operationEnum, currentNum);
        }
        return getResult();
    }

    /**
     * 处理计算保存的实体
     *
     * @param operationEnum 操作类型
     * @param currentNum    当前值
     */
    private void handlerCalc(OperationEnum operationEnum, int currentNum) {
        ValueDto pre = valueDto;
        valueDto = ValueDto.builder()
                .currentNum(currentNum)
                .currentOperation(operationEnum)
                .result(calcResult(operationEnum, currentNum, pre.getResult()))
                .pre(pre)
                .build();
    }

    /**
     * 计算结果
     *
     * @param operationEnum 操作类型
     * @param currentNum    当前值
     * @param preResult     上一步的结果
     * @return 当前计算的结果
     */
    private int calcResult(OperationEnum operationEnum, int currentNum, int preResult) {
        int res = preResult;
        switch (operationEnum) {
            case ADD:
                res = res + currentNum;
                break;
            case SUB:
                res = res - currentNum;
                break;
            case MULTI:
                res = res * currentNum;
                break;
            case DEVI:
                res = res / currentNum;
                break;
            default:
                break;
        }
        return res;
    }

    /***************内部计算所使用到的对象都封装在当前类中，不需要堆外暴露，所以定义为内部类*********************/
    /**
     * 计算器整个操作的相关记录都用该数据结构进行处理
     */
    @Getter
    private static final class ValueDto {
        //当前操作的值 - 用于redo
        private int currentNum;
        //当前操作的类型-用于redo操作
        private OperationEnum operation;
        //当次操作后，计算器的结果值
        private int result;
        //上一步操作的情况-用于undo操作
        private ValueDto pre;

        /**
         * 屏蔽，不能随意变更里面的数据，防止中间过程操作，更改其中的某一个属性值
         */
        private ValueDto() {
        }

        public static ValueDto.Builder builder() {
            return new ValueDto.Builder();
        }

        /**
         * builder模式来构建链表的值
         */
        private static class Builder {
            private int currentNum;
            private OperationEnum operation;
            private int result;
            private ValueDto pre;

            public ValueDto.Builder currentNum(int currentNum) {
                this.currentNum = currentNum;
                return this;
            }

            public ValueDto.Builder result(int result) {
                this.result = result;
                return this;
            }

            public ValueDto.Builder currentOperation(OperationEnum operation) {
                this.operation = operation;
                return this;
            }

            public ValueDto.Builder pre(ValueDto pre) {
                this.pre = pre;
                return this;
            }

            public ValueDto build() {
                ValueDto dto = new ValueDto();
                dto.currentNum = this.currentNum;
                dto.operation = this.operation;
                dto.result = this.result;
                dto.pre = this.pre;
                return dto;
            }
        }
    }

    /**
     * 操作的枚举类
     */
    public enum OperationEnum {
        ADD("加"),
        SUB("减"),
        MULTI("乘"),
        DEVI("除");
        String type;

        OperationEnum(String type) {
            this.type = type;
        }
    }

    /*********************** 所有的操作，相关测试如下 ***************************/
    /**
     * 测试
     *
     * @param args
     */
    public static void main(String[] args) {
        Calculator calculator = Calculator.build();
        System.out.println(calculator.add(1)); //1
        System.out.println(calculator.add(2)); //3
        System.out.println(calculator.sub(1));//2
        System.out.println(calculator.multi(8)); //16
        System.out.println(calculator.devi(4)); //4
        System.out.println(calculator.multi(4)); //16
        System.out.println(calculator.redo()); //64
        System.out.println(calculator.undo()); //16
        System.out.println(calculator.undo()); //4
        System.out.println(calculator.undo()); //16
        System.out.println(calculator.undo()); //2
        System.out.println(calculator.undo()); //3
        System.out.println(calculator.undo()); //1
        System.out.println(calculator.undo()); //0
        System.out.println(calculator.undo()); //0
        System.out.println(calculator.redo()); //0
    }
}
