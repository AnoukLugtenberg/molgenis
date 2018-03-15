// @flow
import api from '@molgenis/molgenis-api-client'
import type { VuexContext } from '../flow.types.js'
import { EntityToFormMapper } from '@molgenis/molgenis-ui-form'

const handleError = (commit: Function, error: Error) => {
  commit('SET_ERROR', error)
  commit('SET_LOADING', false)
}

const actions = {
  'GET_QUESTIONNAIRE_LIST' ({commit}: VuexContext) {
    return api.get('/menu/plugins/questionnaires/list').then(response => {
      commit('SET_QUESTIONNAIRE_LIST', response)
      commit('SET_LOADING', false)
    }, error => {
      handleError(commit, error)
    })
  },

  'START_QUESTIONNAIRE' ({commit, dispatch, getters, state}: VuexContext, questionnaireId: string) {
    const currentQuestionnaireId = getters.getQuestionnaireId
    if (currentQuestionnaireId !== questionnaireId) {
      commit('CLEAR_STATE')
    }

    return api.get(`/menu/plugins/questionnaires/start/${questionnaireId}`).then(() => {
      if (state.chapterFields.length === 0) {
        dispatch('GET_QUESTIONNAIRE', questionnaireId)
      }
    }, error => {
      handleError(commit, error)
    })
  },

  'GET_QUESTIONNAIRE' ({state, commit}: VuexContext, questionnaireId: string) {
    return api.get(`/api/v2/${questionnaireId}?includeCategories=true`).then(response => {
      commit('SET_QUESTIONNAIRE', response)

      const data = response.items.length > 0 ? response.items[0] : {}
      commit('SET_QUESTIONNAIRE_ROW_ID', data[response.meta.idAttribute])

      const form = EntityToFormMapper.generateForm(response.meta, data, state.mapperOptions)
      commit('SET_FORM_DATA', form.formData)

      const chapters = form.formFields.filter(field => field.type === 'field-group')
      commit('SET_CHAPTER_FIELDS', chapters)

      commit('SET_LOADING', false)
    }, error => {
      handleError(commit, error)
    })
  },

  'GET_QUESTIONNAIRE_OVERVIEW' ({commit}: VuexContext, questionnaireId: string) {
    return api.get(`/api/v2/${questionnaireId}`).then(response => {
      commit('SET_QUESTIONNAIRE', response)
      commit('SET_LOADING', false)
    }, error => {
      handleError(commit, error)
    })
  },

  'GET_SUBMISSION_TEXT' ({commit}: VuexContext, questionnaireId: string) {
    return api.get(`/menu/plugins/questionnaires/submission-text/${questionnaireId}`).then(response => {
      commit('SET_SUBMISSION_TEXT', response)
      commit('SET_LOADING', false)
    }, error => {
      handleError(commit, error)
    })
  },

  'AUTO_SAVE_QUESTIONNAIRE' ({commit, state}: VuexContext, formData: Object) {
    const updatedAttribute = Object.keys(formData).find(key => formData[key] !== state.formData[key]) || ''

    const options = {
      body: JSON.stringify(formData[updatedAttribute]),
      method: 'PUT'
    }

    return api.post(`/api/v1/${state.questionnaire.name}/${state.questionnaireRowId}/${updatedAttribute}`, options).then(() => {
      commit('SET_FORM_DATA', formData)
      commit('SET_LOADING', false)
    }).catch(() => {
      // Do not set error because we do not want an error due to auto saving
      commit('SET_LOADING', false)
    })
  },

  'SUBMIT_QUESTIONNAIRE' ({commit, state}: VuexContext, submitDate: string) {
    const submitData = Object.assign({}, state.formData)
    submitData.submitDate = submitDate

    const options = {
      body: JSON.stringify({
        entities: [
          submitData
        ]
      }),
      method: 'PUT'
    }

    return api.post(`/api/v2/${state.questionnaire.name}`, options).then().catch(error => handleError(commit, error))
  }
}

export default actions
