import Vue from 'vue'
import Vuex from 'vuex'

import chat from './modules/chat'
import app from './modules/app'
import user from './modules/user'
import permission from './modules/permission'
import enhance from './modules/enhance'
import online from './modules/online'
import flowable from './modules/flowable'
import getters from './getters'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    chat,
    app,
    user,
    permission,
    enhance,
    online,
    flowable
  },
  state: {

  },
  mutations: {

  },
  actions: {

  },
  getters
})
