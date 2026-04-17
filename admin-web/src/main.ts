import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles/index.css';

const application = createApp(App);

application.use(router);
application.use(ElementPlus);
application.mount('#app');
