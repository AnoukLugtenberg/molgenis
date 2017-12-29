import { createLocalVue, shallow } from 'vue-test-utils'
import Vuex from 'vuex'
import Settings from '@/components/Settings'
import { GET_SETTINGS } from '@/store/actions'
import td from 'testdouble'

const localVue = createLocalVue()
localVue.use(Vuex)

describe('Settings.vue', () => {
  it('should display settings name', () => {
    expect(Settings.name).to.equal('Settings')
  })
  it('should contains created-method content', () => {
    const mockDispatch = td.function('dispatch')
    Settings.$store =
      {
        dispatch: mockDispatch
      }
    Settings.created()
    td.verify(mockDispatch(GET_SETTINGS))
  })
  it('should contains computed-method content', () => {
    let getters = {
      getMappedFields: () => ['field1']
    }
    let store = new Vuex.Store({
      getters
    })
    const wrapper = shallow(Settings, {store, localVue})
    expect(wrapper.vm.schema).to.deep.equal({fields: ['field1']})
  })
})
