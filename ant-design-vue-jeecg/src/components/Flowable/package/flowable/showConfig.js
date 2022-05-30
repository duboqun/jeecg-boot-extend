export default {
  'bpmn:EndEvent': {},
  'bpmn:StartEvent': {
    initiator: true,
    formKey: true
  },
  'bpmn:UserTask': {
    userType: false,
    assignee: false,
    candidateUsers: false,
    candidateGroups: false,
    async: true,
    priority: true,
    formKey: false,
    skipExpression: false,
    dueDate: false,
    taskListener: false
  },
  'bpmn:Task': {
    userType: false,
    assignee: false,
    candidateUsers: false,
    candidateGroups: false,
    async: true,
    priority: true,
    formKey: false,
    skipExpression: false,
    dueDate: false,
    taskListener: false
  },
  'bpmn:ServiceTask': {
    async: true,
    skipExpression: true,
    isForCompensation: true,
    triggerable: true,
    class: true
  },
  'bpmn:ScriptTask': {
    async: true,
    isForCompensation: true,
    autoStoreVariables: true
  },
  'bpmn:ManualTask': {
    async: true,
    isForCompensation: true
  },
  'bpmn:ReceiveTask': {
    async: true,
    isForCompensation: true
  },
  'bpmn:SendTask': {
    async: true,
    isForCompensation: true
  },
  'bpmn:BusinessRuleTask': {
    async: true,
    isForCompensation: true,
    ruleVariablesInput: true,
    rules: true,
    resultVariable: true,
    exclude: true
  }
}
