import Layout from "@/Layout.vue";

const dynamicConst =  [
    {
        path: '',
        component: Layout,
        name: 'container',
        redirect: "/index",
        mate: {
            requireAuth: true,
            name: "主页"
        },
        children: [
            {
                path: '',
            }
        ]
    },
]